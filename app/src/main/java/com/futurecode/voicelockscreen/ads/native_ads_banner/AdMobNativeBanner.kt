package com.futurecode.voicelockscreen.ads.native_ads_banner

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.RatingBar
import android.widget.TextView
import com.futurecode.voicelockscreen.R
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView


class AdMobNativeBanner(private val context: Context) {

    fun loadNativeBannerAd(
        adUnitId: String,
        container: FrameLayout,
        onAdLoaded: ((NativeAd) -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                if (context is Activity && (context.isDestroyed || context.isFinishing)) {
                    nativeAd.destroy()
                    return@forNativeAd
                }

                val adView = LayoutInflater.from(context)
                    .inflate(R.layout.item_admob_native_banner, container, false) as NativeAdView

                populateNativeBannerView(nativeAd, adView)

                container.removeAllViews()
                container.addView(adView)
                container.visibility = View.VISIBLE

                onAdLoaded?.invoke(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    container.visibility = View.GONE
                    onFailed?.invoke(error)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun populateNativeBannerView(
        nativeAd: NativeAd,
        adView: NativeAdView
    ) {
//        val iconView = adView.findViewById<ImageView?>(R.id.ad_app_icon)
        val headlineView = adView.findViewById<TextView?>(R.id.ad_headline)
        val bodyView = adView.findViewById<TextView?>(R.id.ad_body)
        val ctaView = adView.findViewById<Button?>(R.id.ad_call_to_action)
        val mediaView = adView.findViewById<MediaView?>(R.id.ad_media)
        val ratingBar = adView.findViewById<RatingBar?>(R.id.ad_stars)

        requireNotNull(headlineView) { "ad_headline missing in item_admob_native_banner.xml" }
        requireNotNull(ctaView) { "ad_call_to_action missing in item_admob_native_banner.xml" }

        adView.headlineView = headlineView
        adView.callToActionView = ctaView

//        if (iconView != null) adView.iconView = iconView
        if (bodyView != null) adView.bodyView = bodyView
        if (mediaView != null) adView.mediaView = mediaView
        if (ratingBar != null) adView.starRatingView = ratingBar

        headlineView.text = nativeAd.headline

        /*if (iconView != null) {
            if (nativeAd.icon != null) {
                iconView.setImageDrawable(nativeAd.icon!!.drawable)
                iconView.visibility = View.VISIBLE
            } else {
                iconView.visibility = View.GONE
            }
        }*/

        if (bodyView != null) {
            if (!nativeAd.body.isNullOrEmpty()) {
                bodyView.text = nativeAd.body
                bodyView.visibility = View.VISIBLE
            } else {
                bodyView.visibility = View.GONE
            }
        }


        adView.mediaView = mediaView

        if (!nativeAd.callToAction.isNullOrEmpty()) {
            ctaView.text = nativeAd.callToAction
            ctaView.visibility = View.VISIBLE
        } else {
            ctaView.visibility = View.INVISIBLE
        }

        if (ratingBar != null) {
            if (nativeAd.starRating != null) {
                ratingBar.rating = nativeAd.starRating!!.toFloat()
                ratingBar.visibility = View.VISIBLE
            } else {
                ratingBar.visibility = View.GONE
            }
        }

        adView.setNativeAd(nativeAd)
    }
}
