package com.futurecode.voicelockscreen.ads.applovin

import android.content.Context
import android.view.ViewGroup
import com.applovin.mediation.MaxAdFormat

class AppLovinMrecLoader(
    private val context: Context,
    private val adUnitId: String
) {
    private var bannerLoader: AppLovinBannerLoader? = null

    fun loadInto(container: ViewGroup, onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        bannerLoader = AppLovinBannerLoader(context, adUnitId, MaxAdFormat.MREC)
        bannerLoader?.loadInto(container, onLoaded, onFailed)
    }

    fun destroy() {
        bannerLoader?.destroy()
        bannerLoader = null
    }
}