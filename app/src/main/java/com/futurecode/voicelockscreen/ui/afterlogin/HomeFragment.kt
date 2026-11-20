package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentHomeBinding
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.DarkDialogs
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore
import com.futurecode.voicelockscreen.utils.bindBottomTabs

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        VoiceLockFeatureStore.setNotifAsked(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNotificationResultListener()
        setupBackNavigation()
        setupHeaderActions()
        setupVoiceCardActions()
        setupFeatureActions()
        bindBottomTabs(binding.bottomNav, 0)
        setupSwitchActions()
    }

    override fun onResume() {
        super.onResume()
        updateVoiceLockSwitches()

        binding.root.post {
            if (bindingOrNull == null) return@post
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                maybeAskNotifications()
            }
        }
    }

    private fun setupNotificationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            NotificationPermissionDialog.RESULT_KEY,
            viewLifecycleOwner
        ) { _, result ->
            VoiceLockFeatureStore.setNotifAsked(requireContext())
            val allow = result.getBoolean(NotificationPermissionDialog.RESULT_ALLOW)
            if (allow && isNotificationPermissionMissing()) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBackNavigation() {
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
    }

    private fun setupHeaderActions() {
        bindNavigation(binding.ivSettings, R.id.action_homeFragment_to_settingFragment)
        bindNavigation(binding.ivPremium, R.id.action_homeFragment_to_premiumFragment)
    }

    private fun setupVoiceCardActions() {
        // The whole voice card acts as its switch; the reset button re-records the command.
        binding.cardUnlock.setOnClickListener { binding.switchUnlock.performClick() }
        binding.cardLock.setOnClickListener { binding.switchLock.performClick() }
        bindNavigation(binding.btnResetUnlock, R.id.voiceSetupFragment)
        bindNavigation(binding.btnResetLock, R.id.voiceSetupFragment)
    }

    private fun setupFeatureActions() {
        bindNavigation(binding.cardCamera, R.id.action_homeFragment_to_cameraVoiceFragment)

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

        // AI Tools actions
        bindNavigation(binding.cardAiQuotes, R.id.aiQuoteFragment)
        bindNavigation(binding.cardAiVoiceNotes, R.id.action_homeFragment_to_voiceNoteFragment)

        // Lock Screen Themes actions
        bindNavigation(binding.btnSeeAllThemes, R.id.action_homeFragment_to_seeAllthemesFragment)
        bindNavigation(binding.cardTheme1, R.id.action_homeFragment_to_themesFragment)
        bindNavigation(binding.cardTheme2, R.id.action_homeFragment_to_themesFragment)
        bindNavigation(binding.cardTheme3, R.id.action_homeFragment_to_themesFragment)
        bindNavigation(binding.cardTheme4, R.id.action_homeFragment_to_themesFragment)
    }

    private fun bindNavigation(view: View, @IdRes destination: Int) {
        view.setOnClickListener {
            findNavController().navigate(destination)
        }
    }

    private fun setupSwitchActions() {
        binding.switchUnlock.setOnClickListener {
            handleUnlockSwitchChanged(!VoiceLockSwitchStore.isUnlockEffective(requireContext()))
        }
        binding.switchLock.setOnClickListener {
            handleLockSwitchChanged(!VoiceLockSwitchStore.isLockEffective(requireContext()))
        }
        updateVoiceLockSwitches()
    }

    private fun handleUnlockSwitchChanged(enabled: Boolean) {
        VoiceLockSwitchStore.setUnlockEnabled(requireContext(), enabled)

        if (enabled && !SecureVoiceStore.hasPhrase(requireContext())) {
            findNavController().navigate(R.id.voiceSetupFragment)
            return
        }
        if (!enabled) {
            LockService.stop(requireContext())
            updateVoiceLockSwitches()
            return
        }
        if (!hasRequiredPermissions()) {
            openPermissionScreen(PermissionFragment.TARGET_HOME)
            updateVoiceLockSwitches()
            return
        }

        maybeAskNotifications()
        LockService.startIfNeeded(requireContext())
        updateVoiceLockSwitches()
    }

    private fun handleLockSwitchChanged(enabled: Boolean) {
        VoiceLockSwitchStore.setLockEnabled(requireContext(), enabled)

        if (enabled && !SecureVoiceStore.hasPhrase(requireContext())) {
            findNavController().navigate(R.id.voiceSetupFragment)
            return
        }
        if (enabled && !hasRequiredPermissions()) {
            openPermissionScreen(PermissionFragment.TARGET_HOME)
        }
        updateVoiceLockSwitches()
    }

    private fun updateVoiceLockSwitches() {
        renderSwitch(
            binding.switchUnlock,
            binding.knobUnlock,
            binding.tvUnlockSwitchState,
            binding.tvUnlockStatus,
            VoiceLockSwitchStore.isUnlockEffective(requireContext())
        )
        renderSwitch(
            binding.switchLock,
            binding.knobLock,
            binding.tvLockSwitchState,
            binding.tvLockStatus,
            VoiceLockSwitchStore.isLockEffective(requireContext())
        )
    }

    private fun renderSwitch(
        control: View,
        knob: View,
        stateLabel: TextView,
        statusLabel: TextView,
        enabled: Boolean
    ) {
        control.setBackgroundResource(
            if (enabled) R.drawable.bg_home_switch_on else R.drawable.bg_home_switch_off
        )
        knob.setBackgroundResource(
            if (enabled) R.drawable.bg_switch_thumb_white else R.drawable.bg_switch_thumb_lock
        )
        stateLabel.setText(if (enabled) R.string.home_switch_on else R.string.home_switch_off)
        statusLabel.setText(
            if (enabled) R.string.home_status_active else R.string.home_status_inactive
        )
        val statusColor = when {
            statusLabel.id == R.id.tvUnlockStatus -> R.color.white
            enabled -> R.color.success
            else -> R.color.textSecondary
        }
        statusLabel.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
        alignSwitchLabel(stateLabel, enabled)

        val travel = resources.getDimension(R.dimen.home_switch_width) -
            resources.getDimension(R.dimen.home_switch_knob_size) -
            2 * resources.getDimension(R.dimen.spacing_sm)
        val direction = if (control.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1 else 1
        knob.animate()
            .translationX(if (enabled) direction * travel else 0f)
            .setDuration(SWITCH_ANIMATION_DURATION)
            .start()

        control.isSelected = enabled
        ViewCompat.setStateDescription(control, stateLabel.text)
    }

    private fun alignSwitchLabel(label: TextView, enabled: Boolean) {
        val inset = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        label.layoutParams = (label.layoutParams as ConstraintLayout.LayoutParams).apply {
            startToStart = if (enabled) ConstraintSet.PARENT_ID else ConstraintSet.UNSET
            endToEnd = if (enabled) ConstraintSet.UNSET else ConstraintSet.PARENT_ID
            marginStart = if (enabled) inset else 0
            marginEnd = if (enabled) 0 else inset
        }
    }

    private fun openFeatureWithPermission(target: String, @IdRes destination: Int) {
        if (VoiceLockSwitchStore.hasLockPermissions(requireContext())) {
            findNavController().navigate(destination)
        } else {
            openPermissionScreen(target)
        }
    }

    private fun openPermissionScreen(target: String) {
        val arguments = Bundle().apply {
            putString(PermissionFragment.ARG_TARGET, target)
        }
        findNavController().navigate(R.id.action_homeFragment_to_permissionFragment, arguments)
    }

    private fun hasRequiredPermissions() =
        VoiceLockSwitchStore.hasRequiredPermissions(requireContext())

    private fun isNotificationPermissionMissing(): Boolean {
        return Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
    }

    private fun maybeAskNotifications() {
        if (!isNotificationPermissionMissing()) return
        if (VoiceLockFeatureStore.notesAsked(requireContext())) return
        if (parentFragmentManager.isStateSaved) return
        if (parentFragmentManager.findFragmentByTag(NotificationPermissionDialog.TAG) != null) return
        if (findNavController().currentDestination?.id != R.id.homeFragment) return

        NotificationPermissionDialog().show(parentFragmentManager, NotificationPermissionDialog.TAG)
    }

    private companion object {
        const val SWITCH_ANIMATION_DURATION = 200L
    }
}
