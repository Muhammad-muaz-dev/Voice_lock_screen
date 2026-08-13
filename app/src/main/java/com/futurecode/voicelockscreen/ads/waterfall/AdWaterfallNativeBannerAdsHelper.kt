package com.futurecode.voicelockscreen.ads.waterfall

import android.app.Activity
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.NativeAdListener
import com.facebook.ads.NativeBannerAd
import com.facebook.ads.NativeBannerAdView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MainActivity
import com.futurecode.voicelockscreen.ads.applovin.AppLovinMrecLoader
import com.futurecode.voicelockscreen.ads.flow.AdProviderExecutor
import com.futurecode.voicelockscreen.ads.flow.AdProviderType
import com.futurecode.voicelockscreen.ads.flow.AdWaterfallManager
import com.futurecode.voicelockscreen.ads.native_ads_banner.AdMobNativeBanner
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.utils.Utils
import kotlin.run

class AdWaterfallNativeBannerAdsHelper(private val activity: Activity) {
    private val myPreferenceHelper: PrefManager = PrefManager(activity)
    private var nativeBannerAd: NativeBannerAd? = null
    private var appLovinMrecLoader: AppLovinMrecLoader? = null

    fun showNativeBAnnerAd(
        nativeBannerAdView: FrameLayout,
        mainLayout: RelativeLayout,
        placeholder: ImageView
    ) {
        if (myPreferenceHelper.adsOff
        ) {
            hideAd(nativeBannerAdView, mainLayout, placeholder)
            return
        }

        nativeBannerAdView.removeAllViews()
        nativeBannerAdView.background = null
        placeholder.visibility = View.VISIBLE

        AdWaterfallManager.run(
            context = activity,
            flowKey = myPreferenceHelper.nativeBannerAdFlow,
            placementName = "NativeBanner",
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
                Log.d(TAG, "All native banner providers failed: $it")
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

        Log.d(TAG, "showAppLovinMrec: ${myPreferenceHelper.applovinMrec}")

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
        val nativeBannerId = myPreferenceHelper.metaNativeBanner
        if (nativeBannerId.isBlank()) {
            onFailure("Meta native banner id missing")
            return
        }

        val bannerAd = NativeBannerAd(activity, nativeBannerId)
        nativeBannerAd = bannerAd
        val nativeAdListener: NativeAdListener = object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad?) {}

            override fun onError(ad: Ad?, adError: AdError?) {
                nativeBannerAdView.removeAllViews()
                onFailure(adError?.errorMessage ?: "Meta native banner failed")
            }

            override fun onAdLoaded(ad: Ad?) {
                val renderedAd = NativeBannerAdView.render(
                    activity,
                    bannerAd,
                    NativeBannerAdView.Type.HEIGHT_120
                )
                nativeBannerAdView.visibility = View.VISIBLE
                mainLayout.visibility = View.VISIBLE
                placeholder.visibility = View.GONE
                nativeBannerAdView.removeAllViews()
                nativeBannerAdView.addView(renderedAd)
                onSuccess()
            }

            override fun onAdClicked(ad: Ad?) {}
            override fun onLoggingImpression(ad: Ad?) {}
        }

        bannerAd.loadAd(
            bannerAd.buildLoadAdConfig()
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
        val adUnitId = myPreferenceHelper.admobNativeBanner
        if (adUnitId.isBlank()) {
            onFailure("AdMob native banner id missing")
            return
        }

        AdMobNativeBanner(activity).loadNativeBannerAd(
            adUnitId = adUnitId,
            container = nativeBannerAdView,
            onAdLoaded = {
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
        nativeBannerAd?.destroy()
        appLovinMrecLoader?.destroy()
        nativeBannerAd = null
        appLovinMrecLoader = null
    }

    companion object {
        private const val TAG = "AdWaterfallNativeBanner"
    }
}
