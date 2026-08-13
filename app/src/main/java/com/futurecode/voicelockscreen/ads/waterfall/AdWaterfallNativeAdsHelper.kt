package com.futurecode.voicelockscreen.ads.waterfall

import android.app.Activity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.NativeAdListener
import com.facebook.ads.NativeAdView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MainActivity
import com.futurecode.voicelockscreen.ads.applovin.AppLovinMrecLoader
import com.futurecode.voicelockscreen.ads.native_ad.AdMobNative
import com.futurecode.voicelockscreen.ads.flow.AdProviderExecutor
import com.futurecode.voicelockscreen.ads.flow.AdProviderType
import com.futurecode.voicelockscreen.ads.flow.AdWaterfallManager
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.utils.Utils
import com.facebook.ads.NativeAd as MetaNativeAd


class AdWaterfallNativeAdsHelper(private val activity: Activity) {
    private val myPreferenceHelper: PrefManager = PrefManager(activity)
    private var metaNativeAd: MetaNativeAd? = null
    private var appLovinMrecLoader: AppLovinMrecLoader? = null

    fun showNativeAd(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ImageView
    ) {
        if (myPreferenceHelper.adsOff) {
            hideAd(nativeBannerAdView, mainLayout, placeholder)
            return
        }

        nativeBannerAdView.removeAllViews()
        nativeBannerAdView.background = null
        placeholder.visibility = View.VISIBLE

        AdWaterfallManager.run(
            context = activity,
            flowKey = myPreferenceHelper.nativeAdFlow,
            placementName = "Native",
            fallback = listOf(AdProviderType.ADMOB, AdProviderType.META, AdProviderType.CUSTOM),
            executor = AdProviderExecutor { provider, onSuccess, onFailure ->
                when (provider) {
                    AdProviderType.APPLOVIN -> showAppLovinMrec(
                        nativeBannerAdView,
                        mainLayout,
                        placeholder,
                        onSuccess,
                        onFailure
                    )

                    AdProviderType.ADMOB -> showAdmobNative(
                        nativeBannerAdView,
                        mainLayout,
                        placeholder,
                        onSuccess,
                        onFailure
                    )

                    AdProviderType.META -> showMetaNative(
                        nativeBannerAdView,
                        mainLayout,
                        placeholder,
                        onSuccess,
                        onFailure
                    )

                    AdProviderType.CUSTOM -> showCustomNative(
                        nativeBannerAdView,
                        mainLayout,
                        placeholder,
                        onSuccess,
                        onFailure
                    )
                }
            },
            onAllFailed = {
                Log.d(TAG, "All native providers failed: $it")
                hideAd(nativeBannerAdView, mainLayout, placeholder)
            }
        )
    }

    private fun showAppLovinMrec(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ImageView,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        appLovinMrecLoader = AppLovinMrecLoader(activity, myPreferenceHelper.applovinMrec)
        appLovinMrecLoader?.loadInto(
            nativeBannerAdView,
            onLoaded = {
                nativeBannerAdView.visibility = View.VISIBLE
                mainLayout.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
                onSuccess()
            },
            onFailed = onFailure
        )
    }

    private fun showMetaNative(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ImageView,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val nativeId = myPreferenceHelper.metaNative
        if (nativeId.isBlank()) {
            onFailure("Meta native id missing")
            return
        }

        val nativeAd = MetaNativeAd(activity, nativeId)
        metaNativeAd = nativeAd
        val nativeAdListener: NativeAdListener = object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad?) {}

            override fun onError(ad: Ad?, adError: AdError?) {
                nativeBannerAdView.removeAllViews()
                onFailure(adError?.errorMessage ?: "Meta native failed")
            }

            override fun onAdLoaded(ad: Ad?) {
                val adView = NativeAdView.render(activity, nativeAd)
                nativeBannerAdView.visibility = View.VISIBLE
                mainLayout.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
                nativeBannerAdView.removeAllViews()
                nativeBannerAdView.addView(
                    adView,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800)
                )
                onSuccess()
            }

            override fun onAdClicked(ad: Ad?) {}
            override fun onLoggingImpression(ad: Ad?) {}
        }

        nativeAd.loadAd(
            nativeAd.buildLoadAdConfig()
                .withAdListener(nativeAdListener)
                .build()
        )
    }

    private fun showCustomNative(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ImageView,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (myPreferenceHelper.customOff) {
            onFailure("Custom ads are disabled")
            return
        }

        nativeBannerAdView.removeAllViews()
        nativeBannerAdView.visibility = View.GONE
        mainLayout.visibility = View.VISIBLE
        placeholder.visibility = View.VISIBLE
        Glide.with(activity).load(R.drawable.native_thumb).into(placeholder)
        placeholder.setOnClickListener {
            Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
        }
        onSuccess()
    }

    private fun hideAd(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ImageView
    ) {
        if (activity is MainActivity) {
            nativeBannerAdView.removeAllViews()
            nativeBannerAdView.visibility = View.GONE
            mainLayout.visibility = View.GONE
            placeholder.visibility = View.GONE
        } else {
            nativeBannerAdView.removeAllViews()
            nativeBannerAdView.visibility = View.GONE
            mainLayout.visibility = View.GONE
            placeholder.visibility = View.GONE
        }
    }

    private fun showAdmobNative(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ImageView,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val adUnitId = myPreferenceHelper.admobNative
        if (adUnitId.isBlank()) {
            onFailure("AdMob native id missing")
            return
        }

        AdMobNative(activity, myPreferenceHelper).loadNativeAd(
            container = nativeBannerAdView,
            onLoaded = {
                nativeBannerAdView.visibility = View.VISIBLE
                mainLayout.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
                onSuccess()
            },
            onFailed = { error ->
                nativeBannerAdView.removeAllViews()
                onFailure(error.message)
            }
        )
    }

    fun destroy() {
        metaNativeAd?.destroy()
        appLovinMrecLoader?.destroy()
        metaNativeAd = null
        appLovinMrecLoader = null
    }

    companion object {
        private const val TAG = "AdWaterfallNative"
    }
}
