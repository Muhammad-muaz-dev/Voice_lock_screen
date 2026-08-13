package com.futurecode.voicelockscreen.ads.waterfall

import android.app.Activity
import android.util.Log
import com.applovin.mediation.MaxReward
import com.futurecode.voicelockscreen.ads.AdInterface
import com.futurecode.voicelockscreen.ads.LoaderAds
import com.futurecode.voicelockscreen.ads.applovin.AppLovinRewardLoader
import com.futurecode.voicelockscreen.ads.flow.AdProviderExecutor
import com.futurecode.voicelockscreen.ads.flow.AdProviderType
import com.futurecode.voicelockscreen.ads.flow.AdWaterfallManager
import com.futurecode.voicelockscreen.ads.interstitial_ad.CustomFullScreenAdsHelper
import com.futurecode.voicelockscreen.db.PrefManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback



class AdWaterfallRewardAdsHelper(private val activity: Activity) {
    private val myPreferenceHelper: PrefManager = PrefManager(activity)
    private var rewardedAd: RewardedAd? = null
    private var loader: LoaderAds = LoaderAds(activity, false)
    private var adInterface: RewardAdInterface? = null

    fun showRewardAds(adInterface: RewardAdInterface) {
        this.adInterface = adInterface

        if (!myPreferenceHelper.adsOff) {
            this.adInterface?.onAdClosed()
            return
        }

        AdWaterfallManager.run(
            context = activity,
            flowKey = myPreferenceHelper.rewardAdsFlow,
            placementName = "Reward",
            fallback = listOf(AdProviderType.ADMOB),
            executor = AdProviderExecutor { provider, onSuccess, onFailure ->
                when (provider) {
                    AdProviderType.APPLOVIN -> showAppLovinReward(onSuccess, onFailure)
                    AdProviderType.ADMOB -> showAdmobReward(onSuccess, onFailure)
                    AdProviderType.META -> onFailure("Meta rewarded ads are not configured")
                    AdProviderType.CUSTOM -> showCustomRewardFallback(onSuccess, onFailure)
                }
            },
            onAllFailed = {
                loader.dismiss()
                adInterface.onAdFailed(it)
            }
        )
    }

    private fun showAppLovinReward(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        loader.show()
        AppLovinRewardLoader(
            activity,
            myPreferenceHelper.applovinReward
        ).loadAndShow(
            onShown = {
                loader.dismiss()
                adInterface?.onAdShown()
            },
            onClosed = {
                onSuccess()
                adInterface?.onAdClosed()
            },
            onRewarded = { reward ->
                adInterface?.onUserEarnedReward(reward.toRewardItem())
            },
            onFailed = onFailure
        )
    }

    private fun showAdmobReward(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val adUnitId = myPreferenceHelper.admobRewardAd
        if (adUnitId.isBlank()) {
            onFailure("AdMob reward id missing")
            return
        }

        loader.show()
        RewardedAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "AdMob reward failed: ${adError.message}")
                    rewardedAd = null
                    onFailure(adError.message)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    showLoadedAdmobReward(onSuccess, onFailure)
                }
            }
        )
    }

    private fun showLoadedAdmobReward(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onFailure("AdMob reward not ready")
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                onFailure(adError.message)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                onSuccess()
                adInterface?.onAdClosed()
            }

            override fun onAdShowedFullScreenContent() {
                loader.dismiss()
                adInterface?.onAdShown()
            }
        }

        ad.show(activity) { rewardItem ->
            adInterface?.onUserEarnedReward(rewardItem)
        }
    }

    private fun showCustomRewardFallback(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (myPreferenceHelper.customOff) {
            onFailure("Custom ads are disabled")
            return
        }

        loader.show()
        adInterface?.onAdShown()
        CustomFullScreenAdsHelper(activity).show(object : AdInterface {
            override fun finished() {
                loader.dismiss()
                onSuccess()
                adInterface?.onAdClosed()
            }
        })
    }

    private fun MaxReward.toRewardItem(): RewardItem {
        val rewardAmount = amount
        val rewardLabel = label ?: ""
        return object : RewardItem {
            override fun getType(): String = rewardLabel
            override fun getAmount(): Int = rewardAmount
        }
    }

    interface RewardAdInterface {
        fun onAdShown()
        fun onAdClosed()
        fun onAdFailed(error: String)
        fun onUserEarnedReward(rewardItem: RewardItem)
    }

    companion object {
        private const val TAG = "AdWaterfallReward"
    }
}