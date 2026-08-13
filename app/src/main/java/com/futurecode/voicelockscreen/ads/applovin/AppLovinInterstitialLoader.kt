package com.futurecode.voicelockscreen.ads.applovin


import android.app.Activity
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxInterstitialAd

class AppLovinInterstitialLoader(
    private val activity: Activity,
    private val adUnitId: String
) {
    private var interstitialAd: MaxInterstitialAd? = null

    fun loadAndShow(onClosed: () -> Unit, onFailed: (String) -> Unit) {
        if (adUnitId.isBlank()) {
            onFailed("AppLovin interstitial id missing")
            return
        }

        AppLovinInitializer.initialize(activity) { initialized ->
            if (!initialized) {
                onFailed("AppLovin SDK not initialized")
                return@initialize
            }

            val ad = MaxInterstitialAd(adUnitId, activity)
            interstitialAd = ad
            ad.setListener(object : MaxAdListener {
                override fun onAdLoaded(maxAd: MaxAd) {
                    if (ad.isReady) {
                        ad.showAd(activity)
                    } else {
                        onFailed("AppLovin interstitial not ready")
                    }
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    Log.d(TAG, "AppLovin interstitial load failed: ${error.message}")
                    onFailed(error.message ?: "AppLovin interstitial failed")
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    Log.d(TAG, "AppLovin interstitial display failed: ${error.message}")
                    onFailed(error.message ?: "AppLovin interstitial display failed")
                }

                override fun onAdDisplayed(ad: MaxAd) {}
                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) {
                    onClosed()
                }
            })
            ad.loadAd()
        }
    }

    companion object {
        private const val TAG = "AppLovinInterstitial"
    }
}