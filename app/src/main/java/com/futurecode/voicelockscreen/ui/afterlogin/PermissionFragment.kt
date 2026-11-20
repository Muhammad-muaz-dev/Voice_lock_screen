package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPermissionBinding
import com.futurecode.voicelockscreen.utils.LockPermissionHelper

class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        updatePermissionStatus()
        if (hasRequiredPermissions()) navigateToTarget()
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
        if (hasRequiredPermissions()) navigateToTarget()
    }

    private var target: String = TARGET_VOICE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        target = arguments?.getString(ARG_TARGET).orEmpty().ifBlank { TARGET_VOICE }

        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnContinue.setOnClickListener {
            when {
                hasRequiredPermissions() -> navigateToTarget()
                !hasRuntimePermissions() -> requestRequiredPermissions()
                else -> requestOverlayPermission()
            }
        }
        binding.swPhone.setOnClickListener {
            if (!hasPhonePermission()) requestSinglePermission(Manifest.permission.READ_PHONE_STATE)
            updatePermissionStatus()
        }
        binding.swMicrophone.setOnClickListener {
            if (targetNeedsMicrophone() && !hasMicrophonePermission()) requestSinglePermission(Manifest.permission.RECORD_AUDIO)
            updatePermissionStatus()
        }
        binding.swDisplay.setOnClickListener {
            if (!hasOverlayPermission()) requestOverlayPermission()
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
            if (targetNeedsMicrophone() && !hasMicrophonePermission()) add(Manifest.permission.RECORD_AUDIO)
            if (!hasPhonePermission()) add(Manifest.permission.READ_PHONE_STATE)
        }
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestSinglePermission(permission: String) {
        permissionLauncher.launch(arrayOf(permission))
    }

    private fun requestOverlayPermission() {
        overlayPermissionLauncher.launch(LockPermissionHelper.overlaySettingsIntent(requireContext()))
    }

    private fun updatePermissionStatus() {
        val phoneGranted = hasPhonePermission()
        val microphoneGranted = hasMicrophonePermission()
        val overlayGranted = hasOverlayPermission()
        val needsMicrophone = targetNeedsMicrophone()
        val requiredStates = if (needsMicrophone) {
            listOf(phoneGranted, microphoneGranted, overlayGranted)
        } else {
            listOf(phoneGranted, overlayGranted)
        }

        binding.swPhone.isChecked = phoneGranted
        binding.swMicrophone.isChecked = microphoneGranted
        binding.swDisplay.isChecked = overlayGranted
        binding.clMicrophonePermission.visibility = if (needsMicrophone) View.VISIBLE else View.GONE
        binding.dividerPhoneMicrophone.visibility = if (needsMicrophone) View.VISIBLE else View.GONE
        binding.tvEnabledCount.text = getString(
            R.string.permissions_enabled_count,
            requiredStates.count { it },
            requiredStates.size
        )
        binding.btnContinue.isEnabled = true
        binding.btnContinue.alpha = if (hasRequiredPermissions()) 1f else 0.85f
    }

    private fun hasRequiredPermissions(): Boolean = hasRuntimePermissions() && hasOverlayPermission()

    private fun hasRuntimePermissions(): Boolean =
        hasPhonePermission() && (!targetNeedsMicrophone() || hasMicrophonePermission())

    private fun targetNeedsMicrophone(): Boolean = target !in setOf(
        TARGET_PIN,
        TARGET_PATTERN,
        TARGET_FINGERPRINT
    )

    private fun hasPhonePermission(): Boolean = hasPermission(Manifest.permission.READ_PHONE_STATE)

    private fun hasMicrophonePermission(): Boolean = hasPermission(Manifest.permission.RECORD_AUDIO)

    private fun hasOverlayPermission(): Boolean = LockPermissionHelper.hasOverlayPermission(requireContext())

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED

    private fun navigateToTarget() {
        if (target == TARGET_HOME) {
            findNavController().popBackStack()
            return
        }

        val destination = when (target) {
            TARGET_COMPLETE -> R.id.setupCompleteFragment
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
        const val TARGET_COMPLETE = "complete"
        const val ARG_TARGET = "permission_target"
        const val TARGET_VOICE = "voice"
        const val TARGET_PIN = "pin"
        const val TARGET_PATTERN = "pattern"
        const val TARGET_FINGERPRINT = "fingerprint"
        const val TARGET_HOME = "home"
    }
}