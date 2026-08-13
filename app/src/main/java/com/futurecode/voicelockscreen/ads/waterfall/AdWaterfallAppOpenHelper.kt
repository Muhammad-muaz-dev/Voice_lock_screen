package com.futurecode.voicelockscreen.ads.waterfall


import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.ads.AdInterface
import com.futurecode.voicelockscreen.ads.applovin.AppLovinAppOpenLoader
import com.futurecode.voicelockscreen.ads.flow.AdFlowResolver
import com.futurecode.voicelockscreen.ads.flow.AdProviderType
import com.futurecode.voicelockscreen.ads.interstitial_ad.CustomFullScreenAdsHelper
import com.futurecode.voicelockscreen.db.PrefManager

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd


class AdWaterfallAppOpenHelper(private val configs: MyApplication) :
    DefaultLifecycleObserver,
    Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null
    private val sharedPrefs: PrefManager = PrefManager(configs)

    private var isAdShowing = false
    private var isAppOpenFlowInProgress = false
    private var isAppOpenPreloadInProgress = false
    private var appWasInBackground = false
    private var foregroundSessionConsumed = true
    private var hasSeenFirstForeground = false
    private var ignoreNextForegroundAfterAd = false
    private var ignoreNextBackgroundAfterAd = false
    private var lastShowProvider = ""

    private var appLovinLoader: AppLovinAppOpenLoader? = null
    private var appLovinLoadTime = 0L
    private var admobAppOpenAd: AppOpenAd? = null
    private var admobLoadTime = 0L

    init {
        configs.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "AppOpen: foreground event received")

        if (!hasSeenFirstForeground) {
            hasSeenFirstForeground = true
            foregroundSessionConsumed = true
            Log.d(TAG, "AppOpen: first foreground skipped")
            preloadAppOpenAd("first_foreground")
            return
        }

        if (isAdShowing) {
            Log.d(TAG, "AppOpen: skipped, ad already showing")
            return
        }

        if (isAppOpenFlowInProgress) {
            Log.d(TAG, "AppOpen: skipped, flow already in progress")
            return
        }

        if (ignoreNextForegroundAfterAd) {
            ignoreNextForegroundAfterAd = false
            ignoreNextBackgroundAfterAd = false
            appWasInBackground = false
            foregroundSessionConsumed = true
            Log.d(TAG, "AppOpen: resume ignored because it was caused by ad dismiss")
            preloadAppOpenAd("after_ad_resume")
            return
        }

        if (!appWasInBackground) {
            Log.d(TAG, "AppOpen: skipped, app was not backgrounded")
            preloadAppOpenAd("foreground_without_background")
            return
        }

        showAdIfAvailable()
    }

    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "AppOpen: background event received")

        if (isAdShowing || isAppOpenFlowInProgress) {
            Log.d(TAG, "AppOpen: background ignored because app-open ad is active")
            return
        }

        if (ignoreNextBackgroundAfterAd) {
            ignoreNextBackgroundAfterAd = false
            Log.d(TAG, "AppOpen: background ignored because it was caused by ad dismiss")
            return
        }

        appWasInBackground = true
        foregroundSessionConsumed = false
        ignoreNextForegroundAfterAd = false
        Log.d(TAG, "AppOpen: real background detected, next foreground eligible")
        preloadAppOpenAd("background")
    }

    fun showAdIfAvailable() {
        val activity = currentActivity ?: run {
            Log.d(TAG, "AppOpen: skipped, current activity is null")
            preloadAppOpenAd("null_activity")
            return
        }

        if (isAdShowing) {
            Log.d(TAG, "AppOpen: skipped, ad already showing")
            return
        }

        if (isAppOpenFlowInProgress) {
            Log.d(TAG, "AppOpen: skipped, flow already in progress")
            return
        }

        if (foregroundSessionConsumed) {
            Log.d(TAG, "AppOpen: skipped, session is already consumed")
            return
        }

        if (!appWasInBackground) {
            Log.d(TAG, "AppOpen: skipped, no real background entry")
            preloadAppOpenAd("no_background_entry")
            return
        }

        if (!sharedPrefs.adsOff) {
            Log.d(TAG, "AppOpen: skipped, ads are off")
            return
        }

        foregroundSessionConsumed = true
        appWasInBackground = false

        val providers = resolveProviders()
        Log.d(TAG, "AppOpen: checking cached providers")
        showCachedProvider(activity, providers, 0)
    }

    private fun showCachedProvider(
        activity: Activity,
        providers: List<AdProviderType>,
        index: Int
    ) {
        if (index >= providers.size) {
            isAppOpenFlowInProgress = false
            Log.d(TAG, "AppOpen: no cached app-open ad ready, preloading for next foreground")
            preloadAppOpenAd("cache_miss")
            return
        }

        when (val provider = providers[index]) {
            AdProviderType.APPLOVIN -> {
                if (!isAppLovinCachedAdValid()) {
                    Log.d(TAG, "AppOpen: AppLovin cache not ready")
                    showCachedProvider(activity, providers, index + 1)
                    return
                }

                val loader = appLovinLoader ?: run {
                    showCachedProvider(activity, providers, index + 1)
                    return
                }

                isAppOpenFlowInProgress = true
                lastShowProvider = provider.apiName
                Log.d(TAG, "AppOpen: showing cached AppLovin")
                val showStarted = loader.showIfReady(
                    onShown = {
                        markAdDisplayed("AppLovin")
                    },
                    onHidden = {
                        appLovinLoader = null
                        appLovinLoadTime = 0L
                        markAdClosed("AppLovin")
                        preloadAppOpenAd("after_applovin_dismiss")
                    },
                    onFailed = {
                        appLovinLoader = null
                        appLovinLoadTime = 0L
                        markDisplayFailed("AppLovin", it)
                        isAppOpenFlowInProgress = false
                        showCachedProvider(activity, providers, index + 1)
                    }
                )

                if (!showStarted) {
                    isAppOpenFlowInProgress = false
                    appLovinLoader = null
                    appLovinLoadTime = 0L
                    Log.d(TAG, "AppOpen: cached AppLovin was not ready at show time")
                    showCachedProvider(activity, providers, index + 1)
                }
            }

            AdProviderType.ADMOB -> {
                val ad = admobAppOpenAd
                if (ad == null || !isAdmobCachedAdValid()) {
                    Log.d(TAG, "AppOpen: AdMob cache not ready")
                    admobAppOpenAd = null
                    admobLoadTime = 0L
                    showCachedProvider(activity, providers, index + 1)
                    return
                }

                isAppOpenFlowInProgress = true
                lastShowProvider = provider.apiName
                Log.d(TAG, "AppOpen: showing cached AdMob")
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        admobAppOpenAd = null
                        admobLoadTime = 0L
                        markAdClosed("AdMob")
                        preloadAppOpenAd("after_admob_dismiss")
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        admobAppOpenAd = null
                        admobLoadTime = 0L
                        markDisplayFailed("AdMob", adError.message)
                        isAppOpenFlowInProgress = false
                        showCachedProvider(activity, providers, index + 1)
                    }

                    override fun onAdShowedFullScreenContent() {
                        markAdDisplayed("AdMob")
                    }
                }
                ad.show(activity)
            }

            AdProviderType.META -> {
                Log.d(TAG, "AppOpen: Meta app-open is not configured")
                showCachedProvider(activity, providers, index + 1)
            }

            AdProviderType.CUSTOM -> {
                /*showCustomAppOpen(
                    activity = activity,
                    onFinished = {
                        preloadAppOpenAd("after_custom_dismiss")
                    },
                    onFailure = {
                        Log.d(TAG, "AppOpen: Custom failed: $it")
                        showCachedProvider(activity, providers, index + 1)
                    }
                )*/
            }
        }
    }

    private fun preloadAppOpenAd(reason: String) {
        if (!sharedPrefs.adsOff) {
            Log.d(TAG, "AppOpen: preload skipped, ads are off")
            return
        }

        if (isAppOpenPreloadInProgress) {
            Log.d(TAG, "AppOpen: preload skipped, already running ($reason)")
            return
        }

        val providers = resolveProviders()
        if (providers.any { isCachedAdValid(it) }) {
            Log.d(TAG, "AppOpen: preload skipped, cached ad already ready ($reason)")
            return
        }

        isAppOpenPreloadInProgress = true
        Log.d(TAG, "AppOpen: preload started ($reason)")
        preloadProvider(providers, 0)
    }

    private fun preloadProvider(providers: List<AdProviderType>, index: Int) {
        if (index >= providers.size) {
            isAppOpenPreloadInProgress = false
            Log.d(TAG, "AppOpen: preload failed for all providers")
            return
        }

        when (val provider = providers[index]) {
            AdProviderType.APPLOVIN -> preloadAppLovin(
                onLoaded = {
                    isAppOpenPreloadInProgress = false
                    lastShowProvider = provider.apiName
                    Log.d(TAG, "AppOpen: AppLovin preloaded and cached")
                },
                onFailed = {
                    Log.d(TAG, "AppOpen: AppLovin preload failed: $it")
                    preloadProvider(providers, index + 1)
                }
            )

            AdProviderType.ADMOB -> preloadAdmob(
                onLoaded = {
                    isAppOpenPreloadInProgress = false
                    lastShowProvider = provider.apiName
                    Log.d(TAG, "AppOpen: AdMob preloaded and cached")
                },
                onFailed = {
                    Log.d(TAG, "AppOpen: AdMob preload failed: $it")
                    preloadProvider(providers, index + 1)
                }
            )

            AdProviderType.META -> {
                Log.d(TAG, "AppOpen: Meta preload skipped, not configured")
                preloadProvider(providers, index + 1)
            }

            AdProviderType.CUSTOM -> {
                isAppOpenPreloadInProgress = false
                Log.d(TAG, "AppOpen: Custom does not require preload")
            }
        }
    }

    private fun preloadAppLovin(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        val adUnitId = sharedPrefs.applovin_app_open
        if (adUnitId.isBlank()) {
            onFailed("AppLovin app open id missing")
            return
        }

        val loader = appLovinLoader ?: AppLovinAppOpenLoader(configs, adUnitId).also {
            appLovinLoader = it
        }
        loader.preload(
            onLoaded = {
                appLovinLoadTime = System.currentTimeMillis()
                onLoaded()
            },
            onFailed = {
                appLovinLoader = null
                appLovinLoadTime = 0L
                onFailed(it)
            }
        )
    }

    private fun preloadAdmob(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        val adUnitId = sharedPrefs.admobAppOpen
        if (adUnitId.isBlank()) {
            onFailed("AdMob app open id missing")
            return
        }

        AppOpenAd.load(
            configs,
            adUnitId,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    admobAppOpenAd = appOpenAd
                    admobLoadTime = System.currentTimeMillis()
                    onLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    admobAppOpenAd = null
                    admobLoadTime = 0L
                    onFailed(loadAdError.message)
                }
            }
        )
    }

    private fun resolveProviders(): List<AdProviderType> {
        return AdFlowResolver.resolve(
            context = configs,
            flowKey = sharedPrefs.appOpenAdFlow,
            fallback = listOf(AdProviderType.ADMOB, AdProviderType.CUSTOM)
        )
    }

    private fun isCachedAdValid(provider: AdProviderType): Boolean {
        return when (provider) {
            AdProviderType.APPLOVIN -> isAppLovinCachedAdValid()
            AdProviderType.ADMOB -> isAdmobCachedAdValid()
            AdProviderType.META -> false
            AdProviderType.CUSTOM -> false
        }
    }

    private fun isAppLovinCachedAdValid(): Boolean {
        if (appLovinLoader?.isReady() != true) {
            return false
        }

        if (!wasLoadTimeLessThanNHoursAgo(appLovinLoadTime, 4)) {
            appLovinLoader = null
            appLovinLoadTime = 0L
            return false
        }

        return true
    }

    private fun isAdmobCachedAdValid(): Boolean {
        if (admobAppOpenAd == null) {
            return false
        }

        if (!wasLoadTimeLessThanNHoursAgo(admobLoadTime, 4)) {
            admobAppOpenAd = null
            admobLoadTime = 0L
            return false
        }

        return true
    }

    private fun wasLoadTimeLessThanNHoursAgo(loadTime: Long, numHours: Int): Boolean {
        if (loadTime == 0L) {
            return false
        }

        val dateDifference = System.currentTimeMillis() - loadTime
        val numMilliSecondsPerHour = 3600000L
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    private fun markAdDisplayed(providerName: String) {
        isAdShowing = true
        isAppOpenFlowInProgress = true
        foregroundSessionConsumed = true
        appWasInBackground = false
        ignoreNextForegroundAfterAd = false
        ignoreNextBackgroundAfterAd = false
        lastShowProvider = providerName
        Log.d(TAG, "AppOpen: $providerName displayed")
    }

    private fun markAdClosed(providerName: String) {
        isAdShowing = false
        isAppOpenFlowInProgress = false
        foregroundSessionConsumed = true
        appWasInBackground = false
        ignoreNextForegroundAfterAd = true
        ignoreNextBackgroundAfterAd = true
        Log.d(TAG, "AppOpen: $providerName hidden, next resume will be ignored")
    }

    private fun markDisplayFailed(providerName: String, error: String) {
        isAdShowing = false
        Log.d(TAG, "AppOpen: $providerName failed: $error")
    }

    private fun showCustomAppOpen(
        activity: Activity,
        onFinished: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (sharedPrefs.customOff) {
            onFailure("Custom ads are disabled")
            return
        }

        isAppOpenFlowInProgress = true
        markAdDisplayed("Custom")
        CustomFullScreenAdsHelper(activity).show(object : AdInterface {
            override fun finished() {
                markAdClosed("Custom")
                onFinished()
            }
        })
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {
        if (isAdShowing || isAppOpenFlowInProgress) {
            Log.d(TAG, "AppOpen: activity resume ignored during app-open flow")
            return
        }

        if (ignoreNextForegroundAfterAd) {
            ignoreNextForegroundAfterAd = false
            ignoreNextBackgroundAfterAd = false
            appWasInBackground = false
            foregroundSessionConsumed = true
            currentActivity = activity
            Log.d(TAG, "AppOpen: resume ignored because it was caused by ad dismiss")
            preloadAppOpenAd("activity_resume_after_ad")
            return
        }

        currentActivity = activity
        preloadAppOpenAd("activity_resumed")
    }

    override fun onActivityPaused(activity: Activity) {
        if (isAdShowing || isAppOpenFlowInProgress) {
            Log.d(TAG, "AppOpen: activity paused by app-open flow")
        }
    }

    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    companion object {
        private const val TAG = "AdWaterfallAppOpen"
    }
}
