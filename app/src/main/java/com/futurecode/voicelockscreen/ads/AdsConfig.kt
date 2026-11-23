package com.futurecode.voicelockscreen.ads

import com.futurecode.voicelockscreen.BuildConfig
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Ad placement switches from Firebase Remote Config. Every key has a default here, so the app
 * behaves sensibly before any value is published in the Firebase console.
 *
 * The global switch stays on the server JSON (`ads_status`) plus Pro: see [adsAllowed].
 */
object AdsConfig {

    // Keys (publish these in Firebase Remote Config).
    // Per-screen ad JSON (see ScreenAdConfig for the format).
    const val SPLASH_ADS = "splash_ads_config"
    const val LANGUAGE_ADS = "language_ads_config"
    const val ONBOARDING_ADS = "onboarding_ads_config"

    // Screen and placement switches.
    const val INAPP_SCREEN = "inapp_screen_enabled"
    const val LANGUAGE_SCREEN = "language_screen_enabled"
    const val ONBOARDING_SCREEN = "onboarding_enabled"
    const val ONBOARDING_PAGE_PREFIX = "onboarding_page_"         // onboarding_page_1_enabled … _4_
    const val HOME_BANNER = "home_banner_enabled"
    const val COOLDOWN_INTER = "cooldown_inter_enabled"
    const val COOLDOWN_SECONDS = "inter_cooldown_seconds"

    const val ONBOARDING_PAGES = 4

    private val brandCta = MyApplication.app.getColor(R.color.primary)
    private val white = android.graphics.Color.WHITE

    private val splashDefaults = ScreenAdConfig(
        appOpen = true, interstitial = false, banner = false, native = true, timeoutMs = 7000,
        nativeFormat = ScreenAdConfig.FORMAT_COMPACT, ctaColor = brandCta, ctaTextColor = white, ctaTextStyle = "bold"
    )
    private val languageDefaults = splashDefaults.copy(appOpen = false)
    private val onboardingDefaults = splashDefaults.copy(appOpen = false, nativeFormat = ScreenAdConfig.FORMAT_MEDIA)

    private val defaults: Map<String, Any> = buildMap {
        put(SPLASH_ADS, ScreenAdConfig.toJson(splashDefaults))
        put(LANGUAGE_ADS, ScreenAdConfig.toJson(languageDefaults))
        put(ONBOARDING_ADS, ScreenAdConfig.toJson(onboardingDefaults))
        put(INAPP_SCREEN, true)
        put(LANGUAGE_SCREEN, true)
        put(ONBOARDING_SCREEN, true)
        for (page in 1..ONBOARDING_PAGES) put(pageKey(page), true)
        put(HOME_BANNER, true)
        put(COOLDOWN_INTER, true)
        put(COOLDOWN_SECONDS, 30L)
    }

    private val remote: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
                    .build()
            )
            setDefaultsAsync(defaults)
        }
    }

    /** Fetches and activates the latest values, giving up after [timeoutMs] (cached values stay). */
    suspend fun refresh(timeoutMs: Long = 3000) {
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                remote.fetchAndActivate().addOnCompleteListener { if (cont.isActive) cont.resume(Unit) }
            }
        }
    }

    /** Global gate: no ads for Pro users or when the server JSON turns ads off. */
    val adsAllowed: Boolean get() = !MyApplication.app.prefManager.adsOff

    val splashAds: ScreenAdConfig get() = ScreenAdConfig.parse(remote.getString(SPLASH_ADS), splashDefaults)
    val languageAds: ScreenAdConfig get() = ScreenAdConfig.parse(remote.getString(LANGUAGE_ADS), languageDefaults)
    val onboardingAds: ScreenAdConfig get() = ScreenAdConfig.parse(remote.getString(ONBOARDING_ADS), onboardingDefaults)
    val inAppScreen: Boolean get() = remote.getBoolean(INAPP_SCREEN)
    val languageScreen: Boolean get() = remote.getBoolean(LANGUAGE_SCREEN)
    val onboardingScreen: Boolean get() = remote.getBoolean(ONBOARDING_SCREEN) && enabledOnboardingPages().isNotEmpty()
    val homeBanner: Boolean get() = adsAllowed && remote.getBoolean(HOME_BANNER)
    val cooldownInter: Boolean get() = adsAllowed && remote.getBoolean(COOLDOWN_INTER)
    val cooldownMillis: Long get() = remote.getLong(COOLDOWN_SECONDS).coerceAtLeast(0) * 1000

    /** 1-based onboarding page numbers that are switched on. */
    fun enabledOnboardingPages(): List<Int> = (1..ONBOARDING_PAGES).filter { remote.getBoolean(pageKey(it)) }

    private fun pageKey(page: Int) = "${ONBOARDING_PAGE_PREFIX}${page}_enabled"
}
