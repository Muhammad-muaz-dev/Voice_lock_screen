package com.futurecode.voicelockscreen.ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import com.futurecode.voicelockscreen.R
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

/** Renders native and banner ads into placeholder containers. */
object AdViews {

    /**
     * Inflates [layoutRes] (a NativeAdView) into [container], fills it from [ad] and registers it.
     * The previous ad in the container, if any, is destroyed.
     */
    fun showNative(container: ViewGroup, ad: NativeAd, config: ScreenAdConfig) {
        val layoutRes = if (config.nativeHasMedia) R.layout.layout_native_ad_media else R.layout.layout_native_ad_small
        showNative(container, ad, layoutRes)
        container.findViewById<Button>(R.id.adCallToAction)?.let { styleCta(it, config) }
    }

    /** Remote CTA styling: background colour, text colour and weight. */
    private fun styleCta(cta: Button, config: ScreenAdConfig) {
        cta.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(config.ctaColor)
            cornerRadius = cta.resources.getDimension(R.dimen.radius_md)
        }
        cta.setTextColor(config.ctaTextColor)
        val font = when (config.ctaTextStyle) {
            "bold" -> R.font.sora_bold
            "semibold" -> R.font.sora_semibold
            "medium" -> R.font.sora_medium
            else -> R.font.sora_regular
        }
        cta.typeface = androidx.core.content.res.ResourcesCompat.getFont(cta.context, font)
    }

    fun showNative(container: ViewGroup, ad: NativeAd, @LayoutRes layoutRes: Int = R.layout.layout_native_ad_small) {
        clear(container)
        val adView = LayoutInflater.from(container.context).inflate(layoutRes, container, false) as NativeAdView
        val headline = adView.findViewById<TextView>(R.id.adHeadline)
        val body = adView.findViewById<TextView>(R.id.adBody)
        val icon = adView.findViewById<ImageView>(R.id.adIcon)
        val cta = adView.findViewById<Button>(R.id.adCallToAction)
        val media = adView.findViewById<MediaView?>(R.id.adMedia)

        headline.text = ad.headline
        body.text = ad.body
        body.isVisible = !ad.body.isNullOrBlank()
        cta.text = ad.callToAction
        cta.isVisible = !ad.callToAction.isNullOrBlank()
        val iconDrawable = ad.icon?.drawable
        icon.setImageDrawable(iconDrawable)
        icon.isVisible = iconDrawable != null

        adView.headlineView = headline
        adView.bodyView = body
        adView.iconView = icon
        adView.callToActionView = cta
        adView.registerNativeAd(ad, media)

        container.tag = ad
        container.addView(adView)
        container.isVisible = true
    }

    /** Loads an anchored adaptive banner across [container]'s width. [onResult] tells whether it filled. */
    fun loadBanner(activity: Activity, container: ViewGroup, onResult: (Boolean) -> Unit = {}) {
        clear(container)
        if (!AdsConfig.adsAllowed) return onResult(false)
        val widthDp = (activity.resources.displayMetrics.widthPixels / activity.resources.displayMetrics.density).toInt()
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)
        val adView = AdView(activity)
        container.addView(adView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        container.tag = adView
        adView.loadAd(BannerAdRequest.Builder(AdIds.BANNER, size).build(), object : AdLoadCallback<BannerAd> {
            override fun onAdLoaded(ad: BannerAd) {
                container.post {
                    container.isVisible = true
                    onResult(true)
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                container.post {
                    clear(container)
                    onResult(false)
                }
            }
        })
    }

    /** Destroys whatever ad is in [container] and hides it. */
    fun clear(container: ViewGroup) {
        when (val current = container.tag) {
            is NativeAd -> current.destroy()
            is AdView -> current.destroy()
        }
        container.tag = null
        container.removeAllViews()
        container.isVisible = false
    }

    /** Clears [container] when its view is destroyed. */
    fun clearOnDetach(container: ViewGroup) {
        container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                clear(container)
                container.removeOnAttachStateChangeListener(this)
            }
        })
    }
}
