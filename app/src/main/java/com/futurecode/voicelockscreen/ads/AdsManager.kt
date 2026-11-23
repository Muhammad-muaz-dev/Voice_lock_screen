package com.futurecode.voicelockscreen.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Next-Gen Google Mobile Ads: initialization, the splash full-screen ad, the cooldown interstitial
 * and native loading. SDK callbacks arrive on background threads; results are posted to main.
 */
object AdsManager {

    private const val TAG = "AdsManager"
    private val main = Handler(Looper.getMainLooper())
    private val initStarted = AtomicBoolean(false)

    // region Initialization

    /** Call once from Application.onCreate; the SDK is initialized off the main thread. */
    fun initialize(context: Context) {
        if (!initStarted.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        Thread {
            runCatching {
                MobileAds.initialize(appContext, InitializationConfig.Builder(AdIds.APP_ID).build()) {
                    Log.d(TAG, "Mobile Ads initialized")
                }
            }.onFailure { Log.e(TAG, "Mobile Ads init failed", it) }
        }.start()
    }

    // endregion

    // region Splash full-screen (app open or interstitial)

    private var splashAppOpen: AppOpenAd? = null
    private var splashInterstitial: InterstitialAd? = null

    /**
     * Loads the splash full-screen ad from the splash JSON: App Open when AppOpenEnable is 1 (falling
     * back to an interstitial if InterstitialEnable is also 1), else an interstitial. [onFinished]
     * fires once, when an ad is ready or nothing more will load.
     */
    fun loadSplashFullscreen(onFinished: () -> Unit) {
        val finished = once(onFinished)
        splashAppOpen = null
        splashInterstitial = null
        val config = AdsConfig.splashAds
        if (!AdsConfig.adsAllowed) return finished()
        val loadInterstitial = {
            InterstitialAd.load(
                AdRequest.Builder(AdIds.INTERSTITIAL).build(),
                loadCallback(onLoaded = { ad: InterstitialAd -> splashInterstitial = ad }, onFinished = finished)
            )
        }
        when {
            config.appOpen -> AppOpenAd.load(AdRequest.Builder(AdIds.APP_OPEN).build(), object : AdLoadCallback<AppOpenAd> {
                override fun onAdLoaded(ad: AppOpenAd) {
                    main.post { splashAppOpen = ad; finished() }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    main.post { if (config.interstitial) loadInterstitial() else finished() }
                }
            })
            config.interstitial -> loadInterstitial()
            else -> finished()
        }
    }

    /** Shows the loaded splash ad, then calls [onDone]; calls [onDone] right away if none is ready. */
    fun showSplashFullscreen(activity: Activity, onDone: () -> Unit) {
        val done = once(onDone)
        val appOpen = splashAppOpen
        val interstitial = splashInterstitial
        splashAppOpen = null
        splashInterstitial = null
        if (appOpen == null && interstitial == null) {
            done()
            return
        }
        startFullscreenTransition(activity, done) { transition ->
            markFullScreenShown()
            if (appOpen != null) {
                appOpen.adEventCallback = object : AppOpenAdEventCallback {
                    override fun onAdShowedFullScreenContent() {
                        main.post { markFullScreenShown(); transition.onAdOpened() }
                    }

                    override fun onAdDismissedFullScreenContent() {
                        main.post { markFullScreenShown(); transition.finish() }
                    }

                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        main.post { transition.finish() }
                    }
                }
                appOpen.show(activity)
            } else if (interstitial != null) {
                interstitial.adEventCallback = interstitialCallback(transition)
                interstitial.show(activity)
            }
        }
    }

    // endregion

    // region Cooldown interstitial

    private var cooldownAd: InterstitialAd? = null
    private var cooldownLoading = false
    private var lastFullScreenAt = 0L
    private var showingFullScreen = false

    /** Keeps one cooldown interstitial ready in the background. */
    fun preloadCooldownInterstitial() {
        if (!AdsConfig.cooldownInter || cooldownAd != null || cooldownLoading) return
        cooldownLoading = true
        InterstitialAd.load(
            AdRequest.Builder(AdIds.INTERSTITIAL).build(),
            loadCallback(onLoaded = { ad: InterstitialAd -> cooldownAd = ad }, onFinished = { cooldownLoading = false })
        )
    }

    /**
     * Shows the cooldown interstitial when remote config allows it, the cooldown since the last
     * full-screen ad has passed, and an ad is ready. Returns true when an ad is queued to show.
     */
    fun showCooldownInterstitial(activity: Activity): Boolean {
        if (!AdsConfig.cooldownInter || showingFullScreen) return false
        val elapsed = SystemClock.elapsedRealtime() - lastFullScreenAt
        if (lastFullScreenAt != 0L && elapsed < AdsConfig.cooldownMillis) return false
        val ad = cooldownAd
        if (ad == null) {
            preloadCooldownInterstitial()
            return false
        }
        cooldownAd = null
        return startFullscreenTransition(activity, ::preloadCooldownInterstitial) { transition ->
            markFullScreenShown()
            ad.adEventCallback = interstitialCallback(transition)
            ad.show(activity)
        }
    }

    private fun startFullscreenTransition(
        activity: Activity,
        onComplete: () -> Unit,
        show: (FullscreenAdTransition) -> Unit
    ): Boolean {
        if (showingFullScreen) {
            onComplete()
            return false
        }
        showingFullScreen = true
        val transition = FullscreenAdTransition(activity) {
            showingFullScreen = false
            onComplete()
        }
        transition.show { show(transition) }
        return showingFullScreen
    }

    private fun interstitialCallback(transition: FullscreenAdTransition) = object : InterstitialAdEventCallback {
        override fun onAdShowedFullScreenContent() {
            main.post {
                markFullScreenShown()
                transition.onAdOpened()
            }
        }

        override fun onAdDismissedFullScreenContent() {
            // The cooldown counts from when the user is back in the app.
            main.post {
                markFullScreenShown()
                transition.finish()
            }
        }

        override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
            main.post { transition.finish() }
        }
    }

    /** Any full-screen ad, splash included, restarts the cooldown. */
    private fun markFullScreenShown() {
        lastFullScreenAt = SystemClock.elapsedRealtime()
    }

    // endregion

    // region Native

    /** Loads one native ad; [onResult] gets null on failure. Always delivered on main. */
    fun loadNative(adUnitId: String = AdIds.NATIVE, onResult: (NativeAd?) -> Unit) {
        if (!AdsConfig.adsAllowed) return onResult(null)
        val request = NativeAdRequest.Builder(adUnitId, listOf(NativeAd.NativeAdType.NATIVE)).build()
        NativeAdLoader.load(request, object : NativeAdLoaderCallback {
            override fun onNativeAdLoaded(nativeAd: NativeAd) {
                main.post { onResult(nativeAd) }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                main.post { onResult(null) }
            }
        })
    }

    private var languageNative: NativeAd? = null
    private var languageNativeLoading = false
    private val languageNativeWaiters = mutableListOf<(NativeAd?) -> Unit>()

    /** Starts the Language screen's native during splash so it's ready on arrival. */
    fun preloadLanguageNative() {
        if (!AdsConfig.adsAllowed || !AdsConfig.languageAds.native || languageNative != null || languageNativeLoading) return
        languageNativeLoading = true
        loadNative { ad ->
            languageNativeLoading = false
            val waiters = languageNativeWaiters.toList()
            languageNativeWaiters.clear()
            if (waiters.isEmpty()) languageNative = ad else waiters.first()(ad)
        }
    }

    /** Hands over the preloaded language native, waiting for it if it is still loading. */
    fun takeLanguageNative(onResult: (NativeAd?) -> Unit) {
        val ready = languageNative
        when {
            ready != null -> {
                languageNative = null
                onResult(ready)
            }
            languageNativeLoading -> languageNativeWaiters += onResult
            else -> onResult(null)
        }
    }

    // endregion

    private fun <T> loadCallback(onLoaded: (T) -> Unit, onFinished: () -> Unit) = object : AdLoadCallback<T> {
        override fun onAdLoaded(ad: T) {
            main.post { onLoaded(ad); onFinished() }
        }

        override fun onAdFailedToLoad(adError: LoadAdError) {
            main.post(onFinished)
        }
    }

    private fun once(block: () -> Unit): () -> Unit {
        val called = AtomicBoolean(false)
        return { if (called.compareAndSet(false, true)) block() }
    }
}
