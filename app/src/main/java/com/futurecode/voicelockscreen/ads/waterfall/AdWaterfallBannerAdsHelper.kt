package com.futurecode.voicelockscreen.ads.waterfall

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import com.facebook.ads.Ad
import com.facebook.ads.AdError
import com.facebook.ads.AdSize
import com.facebook.ads.AdView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.applovin.AppLovinBannerLoader
import com.futurecode.voicelockscreen.ads.flow.AdProviderExecutor
import com.futurecode.voicelockscreen.ads.flow.AdProviderType
import com.futurecode.voicelockscreen.ads.flow.AdWaterfallManager
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.utils.AppConstants
import com.futurecode.voicelockscreen.utils.Utils

import com.facebook.ads.AdListener as MetaAdListener
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize as GoogleAdSize
import com.google.android.gms.ads.LoadAdError



class AdWaterfallBannerAdsHelper(context: Context, attrs: AttributeSet?) :
    LinearLayout(context, attrs) {

    private val myPreferenceHelper: PrefManager = PrefManager(context)
    private var appLovinLoader: AppLovinBannerLoader? = null
    private var metaAdView: AdView? = null
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
            flowKey = myPreferenceHelper.bannerAdFlow,
            placementName = "Banner",
            fallback = listOf(AdProviderType.ADMOB, AdProviderType.CUSTOM),
            executor = AdProviderExecutor { provider, onSuccess, onFailure ->
                when (provider) {
                    AdProviderType.APPLOVIN -> showAppLovinBanner(context, onSuccess, onFailure)
                    AdProviderType.ADMOB -> showAdmobBanner(context, onSuccess, onFailure)
                    AdProviderType.META -> showMetaBanner(context, onSuccess, onFailure)
                    AdProviderType.CUSTOM -> showCustomAd(context, onSuccess, onFailure)
                }
            },
            onAllFailed = {
                Log.d(TAG, "All banner providers failed: $it")
                removeAllViews()
            }
        )
    }

    private fun showAppLovinBanner(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        appLovinLoader = AppLovinBannerLoader(context, myPreferenceHelper.appLovinBanner)
        appLovinLoader?.loadInto(this, onSuccess, onFailure)
    }

    private fun showMetaBanner(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val metaBannerId = myPreferenceHelper.metaBanner
        if (metaBannerId.isBlank()) {
            onFailure("Meta banner id missing")
            return
        }

        val adView = AdView(context, metaBannerId, AdSize.BANNER_HEIGHT_50)
        metaAdView = adView
        adView.loadAd(
            adView.buildLoadAdConfig()
                .withAdListener(object : MetaAdListener {
                    override fun onError(ad: Ad?, adError: AdError?) {
                        removeAllViews()
                        adView.destroy()
                        onFailure(adError?.errorMessage ?: "Meta banner failed")
                    }

                    override fun onAdLoaded(ad: Ad?) {
                        removeAllViews()
                        addView(adView)
                        onSuccess()
                    }

                    override fun onAdClicked(ad: Ad?) {}
                    override fun onLoggingImpression(ad: Ad?) {}
                })
                .build()
        )
    }

    private fun showAdmobBanner(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val adUnitId = myPreferenceHelper.admobBanner
        if (adUnitId.isBlank()) {
            onFailure("AdMob banner id missing")
            return
        }

        val adView = com.google.android.gms.ads.AdView(context)
        admobAdView = adView
        adView.setAdSize(GoogleAdSize.BANNER)
        adView.adUnitId = adUnitId
        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                removeAllViews()
                adView.destroy()
                onFailure(adError.message)
            }

            override fun onAdLoaded() {
                removeAllViews()
                addView(adView)
                onSuccess()
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    private fun showCustomAd(
        context: Context,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (myPreferenceHelper.customOff) {
            onFailure("Custom ads are disabled")
            return
        }

        orientation = VERTICAL
        removeAllViews()
        val imageView = ImageView(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            setImageResource(R.drawable.banner)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(5, 5, 5, 5)
        }
        addView(imageView)
        imageView.setOnClickListener {
            (context as? Activity)?.let { activity ->
                Utils.openCustomChrome(activity, Utils.getRandomUrls(activity))
            }
        }
        onSuccess()
    }

    override fun onDetachedFromWindow() {
        appLovinLoader?.destroy()
        metaAdView?.destroy()
        admobAdView?.destroy()
        super.onDetachedFromWindow()
    }

    companion object {
        private const val TAG = "AdWaterfallBanner"
    }
}