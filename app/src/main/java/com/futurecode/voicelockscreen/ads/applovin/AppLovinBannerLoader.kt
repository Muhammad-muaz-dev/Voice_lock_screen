package com.futurecode.voicelockscreen.ads.applovin


import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils

class AppLovinBannerLoader(
    private val context: Context,
    private val adUnitId: String,
    private val adFormat: MaxAdFormat = MaxAdFormat.BANNER
) {
    private var adView: MaxAdView? = null

    fun loadInto(container: ViewGroup, onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        if (adUnitId.isBlank()) {
            onFailed("AppLovin ad unit id missing")
            return
        }

        AppLovinInitializer.initialize(context) { initialized ->
            if (!initialized) {
                onFailed("AppLovin SDK not initialized")
                return@initialize
            }

            val view = if (adFormat == MaxAdFormat.MREC) {
                MaxAdView(adUnitId, MaxAdFormat.MREC, context)
            } else {
                MaxAdView(adUnitId, context)
            }
            adView = view
            view.setBackgroundColor(Color.TRANSPARENT)
            view.setListener(object : MaxAdViewAdListener {
                override fun onAdLoaded(ad: MaxAd) {
                    onLoaded()
                }

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) {
                    Log.d(TAG, "AppLovin ${adFormat.label} failed: ${error.message}")
                    container.removeAllViews()
                    view.destroy()
                    onFailed(error.message ?: "AppLovin ${adFormat.label} failed")
                }

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) {
                    Log.d(TAG, "AppLovin ${adFormat.label} display failed: ${error.message}")
                }

                override fun onAdDisplayed(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) {}
                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdExpanded(ad: MaxAd) {}
                override fun onAdCollapsed(ad: MaxAd) {}
            })

            container.removeAllViews()
            container.addView(view, layoutParamsForFormat())
            view.loadAd()
        }
    }

    fun destroy() {
        adView?.destroy()
        adView = null
    }

    private fun layoutParamsForFormat(): ViewGroup.LayoutParams {
        return if (adFormat == MaxAdFormat.MREC) {
            FrameLayout.LayoutParams(
                AppLovinSdkUtils.dpToPx(context, 300),
                AppLovinSdkUtils.dpToPx(context, 250)
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        } else {
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private val MaxAdFormat.label: String
        get() = if (this == MaxAdFormat.MREC) "MREC" else "banner"

    companion object {
        private const val TAG = "AppLovinBannerLoader"
    }
}