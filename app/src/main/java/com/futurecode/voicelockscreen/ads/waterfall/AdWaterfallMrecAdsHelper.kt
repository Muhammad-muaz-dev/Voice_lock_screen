package com.futurecode.voicelockscreen.ads.waterfall

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.applovin.AppLovinMrecLoader
import com.futurecode.voicelockscreen.ads.flow.AdProviderExecutor
import com.futurecode.voicelockscreen.ads.flow.AdProviderType
import com.futurecode.voicelockscreen.ads.flow.AdWaterfallManager
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.utils.AppConstants
import com.futurecode.voicelockscreen.utils.Utils
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError


class AdWaterfallMrecAdsHelper(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {

    private val myPreferenceHelper: PrefManager = PrefManager(context)
    private var appLovinMrecLoader: AppLovinMrecLoader? = null
    private var admobAdView: com.google.android.gms.ads.AdView? = null

    init {
        showAds(context)
    }

    fun showAds(context: Context) {
        if (!myPreferenceHelper.adsOff) {
            removeAllViews()
            return
        }

        AdWaterfallManager.run(
            context = context,
            flowKey = myPreferenceHelper.mrecAdsFlow,
            placementName = "MREC",
            fallback = listOf(AdProviderType.APPLOVIN, AdProviderType.ADMOB, AdProviderType.CUSTOM),
            executor = AdProviderExecutor { provider, onSuccess, onFailure ->
                when (provider) {
                    AdProviderType.APPLOVIN -> showAppLovinMrec(context, onSuccess, onFailure)
                    AdProviderType.ADMOB -> showAdmobMrec(context, onSuccess, onFailure)
                    AdProviderType.META -> onFailure("Meta MREC is not configured")
                    AdProviderType.CUSTOM -> showCustomMrec(context, onSuccess, onFailure)
                }
            },
            onAllFailed = {
                Log.d(TAG, "All MREC providers failed: $it")
                removeAllViews()
            }
        )
    }

    private fun showAppLovinMrec(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        appLovinMrecLoader = AppLovinMrecLoader(
            context,
            myPreferenceHelper.applovinMrec
        )
        appLovinMrecLoader?.loadInto(this, onSuccess, onFailure)
    }

    private fun showAdmobMrec(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val adUnitId = myPreferenceHelper.admobBanner
        if (adUnitId.isBlank()) {
            onFailure("AdMob MREC id missing")
            return
        }

        val adView = com.google.android.gms.ads.AdView(context)
        admobAdView = adView
        adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
        adView.adUnitId = adUnitId
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                removeAllViews()
                adView.destroy()
                onFailure(adError.message)
            }

            override fun onAdLoaded() {
                removeAllViews()
                addView(adView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
                onSuccess()
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    private fun showCustomMrec(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (myPreferenceHelper.customOff) {
            onFailure("Custom ads are disabled")
            return
        }

        val imageView = ImageView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        Glide.with(context).load(R.drawable.native_thumb).into(imageView)
        imageView.setOnClickListener {
            (context as? Activity)?.let { activity ->
                Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
            }
        }
        removeAllViews()
        addView(imageView)
        onSuccess()
    }

    override fun onDetachedFromWindow() {
        appLovinMrecLoader?.destroy()
        admobAdView?.destroy()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val TAG = "AdWaterfallMrec"
    }
}