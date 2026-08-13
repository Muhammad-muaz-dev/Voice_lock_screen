package com.futurecode.voicelockscreen.ads.applovin


import android.app.Activity
import android.util.Log
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxRewardedAd

class AppLovinRewardLoader(
    private val activity: Activity,
    private val adUnitId: String
) {
    private var rewardedAd: MaxRewardedAd? = null

    fun loadAndShow(
        onShown: () -> Unit,
        onClosed: () -> Unit,
        onRewarded: (MaxReward) -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (adUnitId.isBlank()) {
            onFailed("AppLovin reward id missing")
            return
        }

        AppLovinInitializer.initialize(activity) { initialized ->
            if (!initialized) {
                onFailed("AppLovin SDK not initialized")
                return@initialize
            }

            val ad = MaxRewardedAd.getInstance(adUnitId, activity)
            rewardedAd = ad
            ad.setListener(object : MaxRewardedAdListener {
                override fun onAdLoaded(maxAd: MaxAd) {
                    if (ad.isReady) {
                        ad.showAd(activity)
                    } else {
                        onFailed("AppLovin reward not ready")
                    }
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    Log.d(TAG, "AppLovin reward load failed: ${error.message}")
                    onFailed(error.message ?: "AppLovin reward failed")
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    Log.d(TAG, "AppLovin reward display failed: ${error.message}")
                    onFailed(error.message ?: "AppLovin reward display failed")
                }

                override fun onAdDisplayed(ad: MaxAd) {
                    onShown()
                }

                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) {
                    onClosed()
                }

                override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {
                    onRewarded(reward)
                }
            })
            ad.loadAd()
        }
    }

    companion object {
        private const val TAG = "AppLovinReward"
    }
}