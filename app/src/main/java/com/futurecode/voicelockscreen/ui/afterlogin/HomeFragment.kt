package com.futurecode.voicelockscreen.ui.afterlogin

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.adapter.HomeWallpaperAdapter
import com.futurecode.voicelockscreen.api.wallpaper.WallpaperRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.AdPlacement
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentHomeBinding
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.DarkDialogs
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore
import com.futurecode.voicelockscreen.utils.bindBottomTabs

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private var voicePulse: AnimatorSet? = null
    private var homeThemesJob: Job? = null

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
        setupHomeThemes()
        AdPlacement.loadAndShowNative(viewLifecycleOwner, binding.homeNativeAdContainer)
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
        // The toggle (and the mic orb) turn unlock-by-voice on and off.
        val toggle = View.OnClickListener {
            handleUnlockSwitchChanged(!VoiceLockSwitchStore.isUnlockEffective(requireContext()))
        }
        binding.voiceSection.switchVoice.setOnClickListener(toggle)
        binding.voiceSection.voiceMic.setOnClickListener(toggle)
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

        bindNavigation(binding.btnSeeAllThemes, R.id.action_homeFragment_to_seeAllthemesFragment)
    }

    private fun setupHomeThemes() {
        val adapter = HomeWallpaperAdapter(
            onWallpaperClick = { wallpaper ->
                findNavController().navigate(
                    R.id.themePreviewFragment,
                    ThemePreviewFragment.wallpaperArgs(wallpaper)
                )
            },
            onRetryThemes = { loadHomeThemes(forceRefresh = true) }
        ).apply {
            stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT
        }
        binding.rvHomeThemes.adapter = adapter
        loadHomeThemes()
    }

    private fun loadHomeThemes(forceRefresh: Boolean = false) {
        if (homeThemesJob?.isActive == true) return
        val adapter = binding.rvHomeThemes.adapter as HomeWallpaperAdapter
        adapter.submitList(List(HOME_THEME_PLACEHOLDERS) { HomeWallpaperAdapter.Tile.Loading })
        binding.tvHomeThemesMessage.isVisible = false

        homeThemesJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching {
                val category = WallpaperRepository.categories(forceRefresh).firstOrNull()
                    ?: return@runCatching emptyList()
                WallpaperRepository.wallpapers(category.id, forceRefresh).items
            }
            currentCoroutineContext().ensureActive()

            val tiles = result.fold(
                onSuccess = { wallpapers -> wallpapers.map { HomeWallpaperAdapter.Tile.Preview(it) } },
                onFailure = { List(HOME_THEME_PLACEHOLDERS) { HomeWallpaperAdapter.Tile.Retry } }
            )
            adapter.submitList(tiles) {
                adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.ALLOW
            }
            binding.tvHomeThemesMessage.isVisible = result.isSuccess && tiles.isEmpty()
        }
    }

    override fun onDestroyView() {
        homeThemesJob?.cancel()
        binding.rvHomeThemes.adapter = null
        super.onDestroyView()
    }

    private fun bindNavigation(view: View, @IdRes destination: Int) {
        view.setOnClickListener {
            findNavController().navigate(destination)
        }
    }

    private fun setupSwitchActions() {
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

    private fun updateVoiceLockSwitches() {
        val context = requireContext()
        renderVoiceCard(
            hasPhrase = SecureVoiceStore.hasPhrase(context),
            active = VoiceLockSwitchStore.isUnlockEffective(context)
        )
    }

    private fun renderVoiceCard(hasPhrase: Boolean, active: Boolean) {
        val status = when {
            !hasPhrase -> R.string.home_voice_status_line_setup
            active -> R.string.home_voice_status_line_on
            else -> R.string.home_voice_status_line_off
        }
        binding.voiceSection.switchVoice.isChecked = active
        binding.voiceSection.tvVoiceSubtitle.setText(status)
        binding.voiceSection.voiceMic.alpha = if (active) 1f else INACTIVE_MIC_ALPHA
        binding.voiceSection.voiceMic.contentDescription = getString(R.string.home_voice_accessibility_status, getString(R.string.home_voice_title), getString(status))
        startVoiceWaves()
    }

    /**
     * Waves around the mic: three rings keep expanding out of the orb and fading, staggered so a
     * new one starts every third of a cycle. Runs while Home is visible.
     */
    private fun startVoiceWaves() {
        if (voicePulse != null) return
        val waves = listOf(binding.voiceSection.voicePulse, binding.voiceSection.voicePulse2, binding.voiceSection.voicePulse3)
        voicePulse = AnimatorSet().apply {
            playTogether(waves.flatMapIndexed { index, wave ->
                val delay = index * WAVE_DURATION_MS / waves.size
                listOf(
                    pulseAnimator(wave, View.SCALE_X, 1f, WAVE_SCALE, delay),
                    pulseAnimator(wave, View.SCALE_Y, 1f, WAVE_SCALE, delay),
                    pulseAnimator(wave, View.ALPHA, WAVE_START_ALPHA, 0f, delay)
                )
            })
            start()
        }
    }

    private fun pulseAnimator(
        target: View,
        property: android.util.Property<View, Float>,
        from: Float,
        to: Float,
        delay: Long
    ) = ObjectAnimator.ofFloat(target, property, from, to).apply {
        duration = WAVE_DURATION_MS
        startDelay = delay
        repeatCount = ValueAnimator.INFINITE
        interpolator = DecelerateInterpolator()
    }

    private fun stopVoiceWaves() {
        voicePulse?.cancel()
        voicePulse = null
        bindingOrNull?.let { b ->
            listOf(b.voiceSection.voicePulse, b.voiceSection.voicePulse2, b.voiceSection.voicePulse3).forEach { it.alpha = 0f }
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

    override fun onPause() {
        stopVoiceWaves()
        super.onPause()
    }

    private companion object {
        const val HOME_THEME_PLACEHOLDERS = 4
        const val INACTIVE_MIC_ALPHA = 0.55f
        const val WAVE_SCALE = 1.75f
        const val WAVE_START_ALPHA = 0.8f
        const val WAVE_DURATION_MS = 2400L
    }
}
