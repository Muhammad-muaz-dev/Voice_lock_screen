package com.futurecode.voicelockscreen.ads.applovin


import android.content.Context
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAppOpenAd

class AppLovinAppOpenLoader(
    private val context: Context,
    private val adUnitId: String
) {
    private var appOpenAd: MaxAppOpenAd? = null
    private var isLoading = false
    private var onLoadedCallback: (() -> Unit)? = null
    private var onLoadFailedCallback: ((String) -> Unit)? = null
    private var onShownCallback: (() -> Unit)? = null
    private var onHiddenCallback: (() -> Unit)? = null
    private var onShowFailedCallback: ((String) -> Unit)? = null

    fun loadAndShow(onShown: () -> Unit, onHidden: () -> Unit, onFailed: (String) -> Unit) {
        preload(
            onLoaded = {
                showIfReady(onShown, onHidden, onFailed)
            },
            onFailed = onFailed
        )
    }

    fun preload(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        if (adUnitId.isBlank()) {
            onFailed("AppLovin app open id missing")
            return
        }

        if (isReady()) {
            onLoaded()
            return
        }

        if (isLoading) {
            Log.d(TAG, "AppLovin app open preload already running")
            return
        }

        onLoadedCallback = onLoaded
        onLoadFailedCallback = onFailed
        isLoading = true

        AppLovinInitializer.initialize(context) { initialized ->
            if (!initialized) {
                isLoading = false
                onFailed("AppLovin SDK not initialized")
                return@initialize
            }

            val ad = MaxAppOpenAd(adUnitId, context)
            appOpenAd = ad
            ad.setListener(object : MaxAdListener {
                override fun onAdLoaded(maxAd: MaxAd) {
                    isLoading = false
                    Log.d(TAG, "AppLovin app open preloaded")
                    onLoadedCallback?.invoke()
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    isLoading = false
                    Log.d(TAG, "AppLovin app open load failed: ${error.message}")
                    onLoadFailedCallback?.invoke(error.message ?: "AppLovin app open failed")
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    Log.d(TAG, "AppLovin app open display failed: ${error.message}")
                    onShowFailedCallback?.invoke(
                        error.message ?: "AppLovin app open display failed"
                    )
                }

                override fun onAdDisplayed(ad: MaxAd) {
                    Log.d(TAG, "AppLovin app open displayed")
                    onShownCallback?.invoke()
                }

                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) {
                    Log.d(TAG, "AppLovin app open hidden")
                    onHiddenCallback?.invoke()
                    appOpenAd = null
                }
            })
            ad.loadAd()
        }
    }

    fun showIfReady(
        onShown: () -> Unit,
        onHidden: () -> Unit,
        onFailed: (String) -> Unit
    ): Boolean {
        val ad = appOpenAd
        if (ad == null || !ad.isReady) {
            return false
        }

        onShownCallback = onShown
        onHiddenCallback = onHidden
        onShowFailedCallback = onFailed
        ad.showAd("app_open")
        return true
    }

    fun isReady(): Boolean {
        return appOpenAd?.isReady == true
    }

    companion object {
        private const val TAG = "AppLovinAppOpen"
    }
}