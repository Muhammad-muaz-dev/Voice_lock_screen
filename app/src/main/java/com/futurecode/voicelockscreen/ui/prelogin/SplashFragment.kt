package com.futurecode.voicelockscreen.ui.prelogin

import android.animation.ValueAnimator
import android.graphics.Outline
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.AdPlacement
import com.futurecode.voicelockscreen.ads.AdViews
import com.futurecode.voicelockscreen.ads.AdsConfig
import com.futurecode.voicelockscreen.ads.AdsManager
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSplashBinding
import com.futurecode.voicelockscreen.utils.FullBleed
import com.futurecode.voicelockscreen.utils.JsonReadUtils
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Splash: brand violet, app icon and name, a loader, and a bottom native/banner ad.
 * Waits for remote config and the full-screen ad (app open or interstitial), shows it, then
 * continues the launch flow (In-app -> Language -> Onboarding -> Home).
 */
class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private var progressAnimator: ValueAnimator? = null
    private var routed = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        JsonReadUtils.fetchJsonData(requireContext()) { }
        applyBrandBars(true)
        applyInsets()
        roundLogo()
        startProgress()
        AdViews.clearOnDetach(binding.splashAdContainer)
        runLaunchSequence()
    }

    override fun onDestroyView() {
        progressAnimator?.cancel()
        progressAnimator = null
        applyBrandBars(false)
        super.onDestroyView()
    }

    // region Launch sequence

    private fun runLaunchSequence() {
        val startedAt = SystemClock.elapsedRealtime()
        viewLifecycleOwner.lifecycleScope.launch {
            AdsConfig.refresh(REMOTE_TIMEOUT_MS)
            loadBottomAd()
            if (PreloginFlow.languageWillShow()) AdsManager.preloadLanguageNative()
            AdsManager.preloadCooldownInterstitial()

            val fullscreenReady = CompletableDeferred<Unit>()
            AdsManager.loadSplashFullscreen { fullscreenReady.complete(Unit) }
            val maxWait = AdsConfig.splashAds.timeoutMs
            withTimeoutOrNull(maxWait - (SystemClock.elapsedRealtime() - startedAt)) { fullscreenReady.await() }

            val remaining = MIN_DURATION_MS - (SystemClock.elapsedRealtime() - startedAt)
            if (remaining > 0) delay(remaining)
            finishProgress()
            showFullscreenThenRoute()
        }
    }

    private fun showFullscreenThenRoute() {
        val activity = activity ?: return
        AdsManager.showSplashFullscreen(activity) { route() }
    }

    private fun route() {
        if (routed || bindingOrNull == null) return
        routed = true
        PreloginFlow.continueFrom(this, PreloginFlow.Step.SPLASH)
    }

    private fun loadBottomAd() {
        if (!AdsConfig.adsAllowed) return
        AdPlacement.show(this, binding.splashAdContainer, AdsConfig.splashAds)
    }

    // endregion

    // region UI

    /** Violet reaches under the status and navigation bars; icons turn light on it. */
    private fun applyBrandBars(enabled: Boolean) {
        val activity = activity ?: return
        FullBleed.set(activity, enabled)
        WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
            isAppearanceLightStatusBars = !enabled
            isAppearanceLightNavigationBars = !enabled
        }
    }

    /** Keeps the bottom ad above the gesture/navigation bar. */
    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { root, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            root.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun roundLogo() {
        val radius = resources.getDimension(R.dimen.radius_hero)
        binding.ivSplashLogo.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) =
                outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
        binding.ivSplashLogo.clipToOutline = true
    }

    /** Eases towards 90% while ads load; [finishProgress] completes it. */
    private fun startProgress() {
        val max = binding.splashProgress.max
        progressAnimator = ValueAnimator.ofInt(0, (max * 0.9f).toInt()).apply {
            duration = AdsConfig.splashAds.timeoutMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { bindingOrNull?.splashProgress?.progress = it.animatedValue as Int }
            start()
        }
    }

    private fun finishProgress() {
        progressAnimator?.cancel()
        bindingOrNull?.splashProgress?.let { it.progress = it.max }
    }

    // endregion

    private companion object {
        const val REMOTE_TIMEOUT_MS = 3000L
        const val MIN_DURATION_MS = 2200L
    }
}
