package com.futurecode.voicelockscreen.ads.native_ads_banner

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.NativeAd
import com.facebook.ads.NativeAdListener
import com.facebook.ads.NativeAdView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.utils.Utils
import com.google.android.material.imageview.ShapeableImageView

class NativeBannerAdsHelper(private val activity: Activity) {
    private val pref: PrefManager = MyApplication.app.prefManager
    private var metaNativeBannerAd: NativeAd? = null
    private var admobNativeBannerAd: com.google.android.gms.ads.nativead.NativeAd? = null

    fun showNativeBannerAd(
        nativeBannerContainer: FrameLayout,
        customBannerLayout: RelativeLayout,
        bannerPlaceholder: ShapeableImageView
    ) {
        // PREMIUM FEATURE: if (pref.adsOff || MyApplication.app.prefManager.isPro) {
        if (pref.adsOff) {
            hideNativeBanner(nativeBannerContainer, customBannerLayout, bannerPlaceholder)
            return
        }

        clearPreviousNativeBanner(nativeBannerContainer)

        nativeBannerContainer.removeAllViews()
        nativeBannerContainer.background = null
        bannerPlaceholder.visibility = View.VISIBLE

        when (NativeBannerAdsLogic.getCurrentNativeBannerNetwork()) {
            "Admob" -> showAdmobNativeBanner(
                nativeBannerContainer,
                customBannerLayout,
                bannerPlaceholder
            )

            "Meta" -> showMetaNativeBanner(
                nativeBannerContainer,
                customBannerLayout,
                bannerPlaceholder
            )

            "Custom" -> showCustomNativeBanner(
                nativeBannerContainer,
                customBannerLayout,
                bannerPlaceholder
            )

            else -> showCustomNativeBanner(
                nativeBannerContainer,
                customBannerLayout,
                bannerPlaceholder
            )
        }
    }

    private fun showAdmobNativeBanner(
        nativeBannerContainer: FrameLayout,
        customBannerLayout: RelativeLayout,
        bannerPlaceholder: ShapeableImageView
    ) {
        val adUnitId = pref.admobNative
        if (adUnitId.isNullOrBlank()) {
            showCustomNativeBanner(nativeBannerContainer, customBannerLayout, bannerPlaceholder)
            return
        }

        AdMobNativeBanner(activity).loadNativeBannerAd(
            adUnitId = adUnitId,
            container = nativeBannerContainer,
            onAdLoaded = { nativeAd ->
                admobNativeBannerAd?.destroy()
                admobNativeBannerAd = nativeAd

                nativeBannerContainer.visibility = View.VISIBLE
                customBannerLayout.visibility = View.VISIBLE
                bannerPlaceholder.visibility = View.GONE
            },
            onFailed = {
                nativeBannerContainer.removeAllViews()
                nativeBannerContainer.visibility = View.GONE
                showCustomNativeBanner(nativeBannerContainer, customBannerLayout, bannerPlaceholder)
            }
        )
    }

    private fun showMetaNativeBanner(
        nativeBannerContainer: FrameLayout,
        customBannerLayout: RelativeLayout,
        bannerPlaceholder: ShapeableImageView
    ) {
        val placementId = pref.metaNative
        if (placementId.isNullOrBlank()) {
            showCustomNativeBanner(nativeBannerContainer, customBannerLayout, bannerPlaceholder)
            return
        }

        metaNativeBannerAd?.unregisterView()
        metaNativeBannerAd?.destroy()

        val nativeAd = com.facebook.ads.NativeAd(activity, placementId)
        metaNativeBannerAd = nativeAd

        val listener = object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad?) = Unit

            override fun onError(ad: Ad?, adError: AdError?) {
                nativeBannerContainer.removeAllViews()
                showCustomNativeBanner(nativeBannerContainer, customBannerLayout, bannerPlaceholder)
            }

            override fun onAdLoaded(ad: Ad?) {
                if (activity.isDestroyed || activity.isFinishing) {
                    nativeAd.destroy()
                    return
                }

                if (ad !== nativeAd || nativeAd.isAdInvalidated) return

                val adView = NativeAdView.render(activity, nativeAd)

                nativeBannerContainer.removeAllViews()
                nativeBannerContainer.addView(adView)
                nativeBannerContainer.visibility = View.VISIBLE
                customBannerLayout.visibility = View.GONE
                bannerPlaceholder.visibility = View.GONE
            }

            override fun onAdClicked(ad: Ad?) = Unit
            override fun onLoggingImpression(ad: Ad?) = Unit
        }

        nativeAd.loadAd(
            nativeAd.buildLoadAdConfig()
                .withAdListener(listener)
                .build()
        )
    }

    private fun showCustomNativeBanner(
        nativeBannerContainer: FrameLayout,
        customBannerLayout: RelativeLayout,
        bannerPlaceholder: ShapeableImageView
    ) {
        if (pref.customOff) {
            hideNativeBanner(nativeBannerContainer, customBannerLayout, bannerPlaceholder)
            return
        }

        nativeBannerContainer.removeAllViews()
        nativeBannerContainer.visibility = View.GONE
        customBannerLayout.visibility = View.VISIBLE
        bannerPlaceholder.visibility = View.VISIBLE

        Glide.with(activity)
            .load(R.drawable.native_thumb)
            .into(bannerPlaceholder)

        bannerPlaceholder.setOnClickListener {
            Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
        }
    }

    private fun hideNativeBanner(
        nativeBannerContainer: FrameLayout,
        customBannerLayout: RelativeLayout,
        bannerPlaceholder: ShapeableImageView
    ) {
        nativeBannerContainer.removeAllViews()
        nativeBannerContainer.visibility = View.GONE
        customBannerLayout.visibility = View.GONE
        bannerPlaceholder.visibility = View.GONE
    }

    private fun clearPreviousNativeBanner(container: FrameLayout) {
        container.removeAllViews()

        metaNativeBannerAd?.unregisterView()
        metaNativeBannerAd?.destroy()
        metaNativeBannerAd = null

        admobNativeBannerAd?.destroy()
        admobNativeBannerAd = null
    }

    fun onDestroy() {
        metaNativeBannerAd?.unregisterView()
        metaNativeBannerAd?.destroy()
        metaNativeBannerAd = null

        admobNativeBannerAd?.destroy()
        admobNativeBannerAd = null
    }
}
