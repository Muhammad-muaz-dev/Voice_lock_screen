package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPermissionBinding

class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updatePermissionStatus()
        if (hasRequiredPermissions()) {
            navigateToTarget()
        } else if (hasRuntimePermissions() && !hasOverlayPermission()) {
            requestOverlayPermission()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
        if (hasRequiredPermissions()) {
            navigateToTarget()
        }
    }

    private var target: String = TARGET_VOICE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        target = arguments?.getString(ARG_TARGET).orEmpty().ifBlank { TARGET_VOICE }

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            when {
                hasRequiredPermissions() -> navigateToTarget()
                !hasRuntimePermissions() -> requestRequiredPermissions()
                !hasOverlayPermission() -> requestOverlayPermission()
            }
        }

        binding.swPhone.setOnClickListener {
            if (!hasPhonePermission()) requestRequiredPermissions()
            updatePermissionStatus()
        }

        binding.swOverlay.setOnClickListener {
            if (!hasMicrophonePermission()) {
                requestRequiredPermissions()
            } else if (!hasOverlayPermission()) {
                requestOverlayPermission()
            }
            updatePermissionStatus()
        }

        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        if (bindingOrNull != null) updatePermissionStatus()
    }

    private fun requestRequiredPermissions() {
        val permissions = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_PHONE_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        overlayPermissionLauncher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${requireContext().packageName}")
            )
        )
    }

    private fun updatePermissionStatus() {
        val phoneGranted = hasPhonePermission()
        val microphoneGranted = hasMicrophonePermission()
        val overlayGranted = hasOverlayPermission()
        val count = listOf(phoneGranted, microphoneGranted).count { it }

        binding.swPhone.setOnCheckedChangeListener(null)
        binding.swOverlay.setOnCheckedChangeListener(null)
        binding.swPhone.isChecked = phoneGranted
        binding.swOverlay.isChecked = microphoneGranted && overlayGranted

        binding.tvEnabledCount.text = getString(R.string.permissions_enabled_count, count, REQUIRED_PERMISSION_COUNT)
        binding.btnContinue.isEnabled = true
        binding.btnContinue.alpha = if (hasRequiredPermissions()) 1f else 0.85f
    }

    private fun hasRequiredPermissions(): Boolean = hasRuntimePermissions() && hasOverlayPermission()

    private fun hasRuntimePermissions(): Boolean = hasPhonePermission() && hasMicrophonePermission()

    private fun hasPhonePermission(): Boolean = hasPermission(Manifest.permission.READ_PHONE_STATE)

    private fun hasMicrophonePermission(): Boolean = hasPermission(Manifest.permission.RECORD_AUDIO)

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(requireContext())
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun navigateToTarget() {
        val destination = when (target) {
            TARGET_PIN -> R.id.pinLockFragment
            TARGET_PATTERN -> R.id.patternLockFragment
            TARGET_FINGERPRINT -> R.id.fingerprintLockFragment
            TARGET_VOICE -> R.id.voiceSetupFragment
            else -> R.id.voiceSetupFragment
        }

        if (findNavController().currentDestination?.id == R.id.permissionFragment) {
            findNavController().navigate(destination)
        } else {
            Toast.makeText(requireContext(), R.string.permissions_required, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ARG_TARGET = "permission_target"
        const val TARGET_VOICE = "voice"
        const val TARGET_PIN = "pin"
        const val TARGET_PATTERN = "pattern"
        const val TARGET_FINGERPRINT = "fingerprint"
        private const val REQUIRED_PERMISSION_COUNT = 2
    }
}
