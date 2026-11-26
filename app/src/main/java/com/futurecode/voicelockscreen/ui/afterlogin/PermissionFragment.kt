package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.activity.OverlayGuideActivity
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPermissionBinding
import com.futurecode.voicelockscreen.utils.LockPermissionHelper

/**
 * Permissions Required. Each flow passes [ARG_TARGET]; only the permissions that flow needs are
 * shown and requested:
 * - voice flows ([TARGET_VOICE], [TARGET_COMPLETE], [TARGET_HOME]): microphone, phone, overlay
 * - [TARGET_PIN], [TARGET_PATTERN], [TARGET_FINGERPRINT]: phone, overlay
 */
class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {

    private enum class Need { MICROPHONE, PHONE, OVERLAY }

    private var target: String = TARGET_VOICE
    private val guideHandler = Handler(Looper.getMainLooper())

    private val runtimePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onPermissionsChanged()
        if (results[Manifest.permission.READ_PHONE_STATE] == false) {
            showPhonePermissionSettingsPrompt()
        }
    }
    private val phoneSettings = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { onPermissionsChanged() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        target = arguments?.getString(ARG_TARGET).orEmpty().ifBlank { TARGET_VOICE }
        setupRows()
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnContinue.setOnClickListener { onContinue() }
        render()
    }

    override fun onResume() {
        super.onResume()
        // Returning from system Settings (overlay or phone access) lands here.
        if (bindingOrNull != null) render()
    }

    override fun onDestroyView() {
        guideHandler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    // region Setup

    private fun needs(): List<Need> = when (target) {
        TARGET_PIN, TARGET_PATTERN, TARGET_FINGERPRINT -> listOf(Need.PHONE, Need.OVERLAY)
        else -> listOf(Need.MICROPHONE, Need.PHONE, Need.OVERLAY)
    }

    private fun setupRows() {
        val needed = needs()
        binding.clMicrophonePermission.isVisible = Need.MICROPHONE in needed
        binding.clPhonePermission.isVisible = Need.PHONE in needed
        binding.clDisplayPermission.isVisible = Need.OVERLAY in needed

        val microphone = { request(Need.MICROPHONE) }
        val phone = { request(Need.PHONE) }
        val overlay = { request(Need.OVERLAY) }
        binding.clMicrophonePermission.setOnClickListener { microphone() }
        binding.swMicrophone.setOnClickListener { microphone() }
        binding.clPhonePermission.setOnClickListener { phone() }
        binding.swPhone.setOnClickListener { phone() }
        binding.clDisplayPermission.setOnClickListener { overlay() }
        binding.swDisplay.setOnClickListener { overlay() }
    }

    // endregion

    // region Actions

    private fun onContinue() {
        val missing = needs().firstOrNull { !isGranted(it) }
        if (missing == null) navigateToTarget() else request(missing)
    }

    private fun request(need: Need) {
        if (isGranted(need)) {
            render()
            return
        }
        when (need) {
            Need.MICROPHONE -> runtimePermissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            Need.PHONE -> runtimePermissions.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
            Need.OVERLAY -> openOverlaySettings()
        }
    }

    private fun onPermissionsChanged() {
        if (bindingOrNull == null) return
        render()
        if (needs().all(::isGranted)) navigateToTarget()
    }

    private fun showPhonePermissionSettingsPrompt() {
        if (!isAdded || bindingOrNull == null) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.phone_call_access)
            .setMessage(R.string.phone_call_desc)
            .setNegativeButton(R.string.not_now, null)
            .setPositiveButton(R.string.open_app_settings) { _, _ -> openPhonePermissionSettings() }
            .show()
    }

    private fun openPhonePermissionSettings() {
        val context = requireContext()
        val opened = runCatching {
            phoneSettings.launch(LockPermissionHelper.appDetailsSettingsIntent(context))
        }
        if (opened.isFailure) {
            Toast.makeText(context, R.string.permissions_required, Toast.LENGTH_SHORT).show()
        }
    }

    /** System overlay page, with our "find Voice Lock and turn it on" guide on top of it. */
    private fun openOverlaySettings() {
        val activity = requireActivity()
        val opened = runCatching { activity.startActivity(LockPermissionHelper.overlaySettingsIntent(activity)) }
        if (opened.isFailure) {
            Toast.makeText(activity, R.string.permissions_required, Toast.LENGTH_SHORT).show()
            return
        }
        guideHandler.postDelayed({
            if (!activity.isFinishing && !activity.isDestroyed) {
                activity.startActivity(Intent(activity, OverlayGuideActivity::class.java))
            }
        }, OVERLAY_GUIDE_DELAY_MS)
    }

    private fun navigateToTarget() {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.permissionFragment) return
        if (target == TARGET_HOME) {
            LockService.startIfNeeded(requireContext())
            navController.popBackStack()
            return
        }
        if (target == TARGET_COMPLETE) {
            completeVoiceSetup()
            return
        }
        val destination = when (target) {
            TARGET_PIN -> R.id.pinLockFragment
            TARGET_PATTERN -> R.id.patternLockFragment
            TARGET_FINGERPRINT -> R.id.fingerprintLockFragment
            else -> R.id.voiceSetupFragment
        }
        // Drop this screen once its job is done, so Back from the next step doesn't land here.
        val options = androidx.navigation.navOptions {
            anim {
                enter = R.anim.vn_enter_from_right
                exit = R.anim.vn_exit_to_left
                popEnter = R.anim.vn_enter_from_left
                popExit = R.anim.vn_exit_to_right
            }
            popUpTo(R.id.permissionFragment) { inclusive = true }
        }
        navController.navigate(destination, null, options)
    }

    // endregion

    // region State

    private fun render() {
        binding.swMicrophone.isChecked = isGranted(Need.MICROPHONE)
        binding.swPhone.isChecked = isGranted(Need.PHONE)
        binding.swDisplay.isChecked = isGranted(Need.OVERLAY)
        binding.btnContinue.alpha = if (needs().all(::isGranted)) 1f else PARTIAL_ALPHA
    }

    private fun isGranted(need: Need): Boolean = when (need) {
        Need.MICROPHONE -> hasPermission(Manifest.permission.RECORD_AUDIO)
        Need.PHONE -> hasPermission(Manifest.permission.READ_PHONE_STATE)
        Need.OVERLAY -> LockPermissionHelper.hasOverlayPermission(requireContext())
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED

    // endregion

    companion object {
        const val ARG_TARGET = "permission_target"
        const val TARGET_COMPLETE = "complete"
        const val TARGET_VOICE = "voice"
        const val TARGET_PIN = "pin"
        const val TARGET_PATTERN = "pattern"
        const val TARGET_FINGERPRINT = "fingerprint"
        const val TARGET_HOME = "home"
        private const val OVERLAY_GUIDE_DELAY_MS = 700L
        private const val PARTIAL_ALPHA = 0.85f
    }
}
