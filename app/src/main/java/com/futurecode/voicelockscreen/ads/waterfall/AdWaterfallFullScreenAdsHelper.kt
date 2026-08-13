package com.futurecode.voicelockscreen.ads.waterfall

import android.app.Activity
import android.util.Log
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.InterstitialAd
import com.facebook.ads.InterstitialAdListener
import com.futurecode.voicelockscreen.ads.AdInterface
import com.futurecode.voicelockscreen.ads.LoaderAds
import com.futurecode.voicelockscreen.ads.applovin.AppLovinInterstitialLoader
import com.futurecode.voicelockscreen.ads.flow.AdProviderExecutor
import com.futurecode.voicelockscreen.ads.flow.AdProviderType
import com.futurecode.voicelockscreen.ads.flow.AdWaterfallManager
import com.futurecode.voicelockscreen.ads.interstitial_ad.CustomFullScreenAdsHelper
import com.futurecode.voicelockscreen.db.PrefManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdWaterfallFullScreenAdsHelper(private val activity: Activity) {
    private var myPreferenceHelper: PrefManager = PrefManager(activity)
    private var loader: LoaderAds = LoaderAds(activity, false)
    private lateinit var adInterface: AdInterface
    private var metaInterstitialAd: InterstitialAd? = null
    private var admobInterstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd? = null

    fun showInterstitialAds(isShowEveryTime: Boolean, adInterface: AdInterface) {
        this.adInterface = adInterface
        try {
            val limit = myPreferenceHelper.adFrequency
            val count: Int = myPreferenceHelper.adShowCounter

            if (isShowEveryTime || limit <= 0 || count % limit == 0) {
                showAds()
            } else {
                openActivity()
            }

            if (!isShowEveryTime) {
                myPreferenceHelper.adShowCounter = count + 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showAds()
        }
    }

    private fun showAds() {
        if (myPreferenceHelper.adsOff) {
            openActivity()
            return
        }

        AdWaterfallManager.run(
            context = activity,
            flowKey = myPreferenceHelper.interstitialAdsFlow,
            placementName = "Interstitial",
            fallback = listOf(AdProviderType.ADMOB, AdProviderType.META, AdProviderType.CUSTOM),
            executor = AdProviderExecutor { provider, onSuccess, onFailure ->
                when (provider) {
                    AdProviderType.APPLOVIN -> showAppLovinAds(onSuccess, onFailure)
                    AdProviderType.ADMOB -> showAdmobAds(onSuccess, onFailure)
                    AdProviderType.META -> showMetaAds(onSuccess, onFailure)
                    AdProviderType.CUSTOM -> showCustomAd(onSuccess, onFailure)
                }
            },
            onAllFailed = {
                Log.d(TAG, "All interstitial providers failed: $it")
                openActivity()
            }
        )
    }

    private fun showAppLovinAds(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        loader.show()
        AppLovinInterstitialLoader(
            activity,
            myPreferenceHelper.applovinInterstitial
        ).loadAndShow(
            onClosed = {
                loader.dismiss()
                onSuccess()
                openActivity()
            },
            onFailed = onFailure
        )
    }

    private fun showMetaAds(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val interstitialId: String = myPreferenceHelper.metaInterstitial
        if (interstitialId.isBlank()) {
            onFailure("Meta interstitial id missing")
            return
        }

        loader.show()
        metaInterstitialAd = InterstitialAd(activity, interstitialId)
        val interstitialAdListener: InterstitialAdListener = object : InterstitialAdListener {
            override fun onInterstitialDisplayed(ad: Ad?) {}

            override fun onInterstitialDismissed(ad: Ad?) {
                loader.dismiss()
                onSuccess()
                openActivity()
            }

            override fun onError(ad: Ad?, adError: AdError?) {
                onFailure(adError?.errorMessage ?: "Meta interstitial failed")
            }

            override fun onAdLoaded(ad: Ad?) {
                loader.dismiss()
                metaInterstitialAd?.show()
            }

            override fun onAdClicked(ad: Ad?) {}
            override fun onLoggingImpression(ad: Ad?) {}
        }

        val metaAd = metaInterstitialAd ?: run {
            onFailure("Meta interstitial not ready")
            return
        }
        metaAd.loadAd(
            metaAd.buildLoadAdConfig()
                .withAdListener(interstitialAdListener)
                .build()
        )
    }

    private fun showAdmobAds(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val adUnitId = myPreferenceHelper.admobInterstitial
        if (adUnitId.isBlank()) {
            onFailure("AdMob interstitial id missing")
            return
        }

        loader.show()
        val adRequest = AdRequest.Builder().build()
        com.google.android.gms.ads.interstitial.InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Log.d(TAG, "AdMob interstitial failed: ${loadAdError.message}")
                    admobInterstitialAd = null
                    onFailure(loadAdError.message)
                }

                override fun onAdLoaded(interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd) {
                    admobInterstitialAd = interstitialAd
                    interstitialAd.fullScreenContentCallback =
                        object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                loader.dismiss()
                                admobInterstitialAd = null
                                onSuccess()
                                openActivity()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                                admobInterstitialAd = null
                                onFailure(adError.message)
                            }
                        }
                    loader.dismiss()
                    admobInterstitialAd?.show(activity) ?: onFailure("AdMob interstitial not ready")
                }
            }
        )
    }

    private fun showCustomAd(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (myPreferenceHelper.customOff) {
            onFailure("Custom ads are disabled")
            return
        }

        loader.show()
        CustomFullScreenAdsHelper(activity).show(object : AdInterface {
            override fun finished() {
                loader.dismiss()
                onSuccess()
                openActivity()
            }
        })
    }

    fun showCustomAdOnly(showLoader: Boolean, adInterface: AdInterface) {
        this.adInterface = adInterface
        if (showLoader) {
            loader.show()
        }

        if (myPreferenceHelper.customOff) {
            CustomFullScreenAdsHelper(activity).show(object : AdInterface {
                override fun finished() {
                    loader.dismiss()
                    openActivity()
                }
            })
        } else {
            openActivity()
        }
    }

    private fun openActivity() {
        loader.dismiss()
        metaInterstitialAd?.destroy()
        metaInterstitialAd = null
        if (this::adInterface.isInitialized) {
            adInterface.finished()
        }
    }

    companion object {
        private const val TAG = "AdWaterfallFullScreen"
    }
}
