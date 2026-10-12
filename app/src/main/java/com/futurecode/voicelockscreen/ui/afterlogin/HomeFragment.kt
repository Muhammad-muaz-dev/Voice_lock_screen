package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentHomeBinding
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.DarkDialogs
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { VoiceLockFeatureStore.setNotifAsked(requireContext()) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    DarkDialogs.show(
                        requireContext(),
                        getString(R.string.exit_app_title),
                        getString(R.string.exit_app_body),
                        getString(R.string.stay),
                        getString(R.string.exit),
                        onPositive = { requireActivity().finish() }
                    )
                }
            }
        )

        // Header Actions
        binding.ivSettings.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)
        }

        binding.ivPremium.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_premiumFragment)
        }

        // Voice Unlock Card Setup
        binding.cardUnlock.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_voiceSetupFragment)
        }

        // 2x2 Grid Tools
        binding.cardCamera.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_cameraVoiceFragment)
        }

        binding.pinLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_PIN,
                R.id.action_homeFragment_to_pinLockFragment
            )
        }

        binding.patternLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_PATTERN,
                R.id.action_homeFragment_to_patternLockFragment
            )
        }

        binding.fingerLock.setOnClickListener {
            openFeatureWithPermission(
                PermissionFragment.TARGET_FINGERPRINT,
                R.id.action_homeFragment_to_fingerprintLockFragment
            )
        }
        binding.cardLock.setOnClickListener { findNavController().navigate(R.id.voiceGuideFragment) }

        // Bottom Navigation Tabs
        binding.tabVoiceLock.setOnClickListener {
            // Already on Voice Lock home
        }

        binding.tabThemes.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_themesFragment)
        }

        binding.tabVoiceNote.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_voiceNoteFragment)
        }

        bindVoiceLockSwitches()
    }

    override fun onResume() {
        super.onResume()
        updateVoiceLockSwitches()
    }

    private fun bindVoiceLockSwitches() {
        binding.switchUnlock.setOnClickListener {
            val newState = !VoiceLockSwitchStore.isUnlockEffective(requireContext())
            handleUnlockSwitchChanged(newState)
        }

        binding.switchLock.setOnClickListener {
            val newState = !VoiceLockSwitchStore.isLockEffective(requireContext())
            handleLockSwitchChanged(newState)
        }

        updateVoiceLockSwitches()
    }

    private fun handleUnlockSwitchChanged(enabled: Boolean) {
        VoiceLockSwitchStore.setUnlockEnabled(requireContext(), enabled)
        if (enabled && !com.futurecode.voicelockscreen.security.SecureVoiceStore.hasPhrase(requireContext())) {
            findNavController().navigate(R.id.voiceGuideFragment)
            return
        }
        if (!enabled) {
            LockService.stop(requireContext())
            updateVoiceLockSwitches()
            return
        }

        if (!hasRequiredPermissions()) {
            openPermissionScreenForHomeSwitch()
            updateVoiceLockSwitches()
            return
        }

        maybeAskNotifications()
        LockService.startIfNeeded(requireContext())
        updateVoiceLockSwitches()
    }

    private fun handleLockSwitchChanged(enabled: Boolean) {
        VoiceLockSwitchStore.setLockEnabled(requireContext(), enabled)
        if (enabled && !com.futurecode.voicelockscreen.security.SecureVoiceStore.hasPhrase(requireContext())) {
            findNavController().navigate(R.id.voiceGuideFragment)
            return
        }
        if (enabled && !hasRequiredPermissions()) {
            openPermissionScreenForHomeSwitch()
        }
        updateVoiceLockSwitches()
    }

    private fun updateVoiceLockSwitches() {
        val unlockChecked = VoiceLockSwitchStore.isUnlockEffective(requireContext())
        val lockChecked = VoiceLockSwitchStore.isLockEffective(requireContext())
        val shiftDistance = 26f * resources.displayMetrics.density

        // 1. Render Unlock Switch (60x34dp, knob 28x28dp)
        if (unlockChecked) {
            binding.switchUnlock.setBackgroundResource(R.drawable.bg_switch_track_unlock_active)
            binding.knobUnlock.setBackgroundResource(R.drawable.bg_switch_thumb_white)
            binding.knobUnlock.animate().translationX(shiftDistance).setDuration(200).start()
            binding.tvUnlockStatus.setText(R.string.status_active)
        } else {
            binding.switchUnlock.setBackgroundResource(R.drawable.bg_switch_track_unlock)
            binding.knobUnlock.setBackgroundResource(R.drawable.bg_switch_thumb_lock)
            binding.knobUnlock.animate().translationX(0f).setDuration(200).start()
            binding.tvUnlockStatus.setText(R.string.status_inactive)
        }

        // 2. Render Lock Switch (60x34dp, knob 28x28dp)
        if (lockChecked) {
            binding.switchLock.setBackgroundResource(R.drawable.bg_switch_track_lock_active)
            binding.knobLock.setBackgroundResource(R.drawable.bg_switch_thumb_white)
            binding.knobLock.animate().translationX(shiftDistance).setDuration(200).start()
            binding.tvLockStatus.setText(R.string.status_active)
        } else {
            binding.switchLock.setBackgroundResource(R.drawable.bg_switch_track_lock)
            binding.knobLock.setBackgroundResource(R.drawable.bg_switch_thumb_lock)
            binding.knobLock.animate().translationX(0f).setDuration(200).start()
            binding.tvLockStatus.setText(R.string.status_inactive)
        }
    }

    private fun openPermissionScreenForHomeSwitch() {
        findNavController().navigate(
            R.id.action_homeFragment_to_permissionFragment,
            Bundle().apply {
                putString(PermissionFragment.ARG_TARGET, PermissionFragment.TARGET_HOME)
            }
        )
    }

    private fun openFeatureWithPermission(target: String, @IdRes directActionId: Int) {
        if (hasRequiredPermissions()) {
            findNavController().navigate(directActionId)
            return
        }

        findNavController().navigate(
            R.id.action_homeFragment_to_permissionFragment,
            Bundle().apply {
                putString(PermissionFragment.ARG_TARGET, target)
            }
        )
    }

    private fun hasRequiredPermissions(): Boolean {
        return VoiceLockSwitchStore.hasRequiredPermissions(requireContext())
    }

    private fun maybeAskNotifications() {
        if (VoiceLockFeatureStore.notesAsked(requireContext())) return
        DarkDialogs.show(
            requireContext(),
            getString(R.string.want_security_alerts),
            getString(R.string.security_alerts_body),
            getString(R.string.not_now),
            getString(R.string.allow),
            onNegative = { VoiceLockFeatureStore.setNotifAsked(requireContext()) }
        ) {
            VoiceLockFeatureStore.setNotifAsked(requireContext())
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
