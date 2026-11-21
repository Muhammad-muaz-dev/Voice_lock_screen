package com.futurecode.voicelockscreen.ads

import android.view.ViewGroup
import androidx.fragment.app.Fragment

/** Fills a screen's bottom ad slot from its JSON: native when NativeEnable, else banner when BannerEnable. */
object AdPlacement {

    fun show(fragment: Fragment, container: ViewGroup, config: ScreenAdConfig) {
        if (!AdsConfig.adsAllowed) return
        when {
            config.native -> AdsManager.loadNative { ad ->
                val alive = fragment.view != null && container.isAttachedToWindow
                when {
                    ad == null -> Unit
                    !alive -> ad.destroy()
                    else -> AdViews.showNative(container, ad, config)
                }
            }
            config.banner -> fragment.activity?.let { AdViews.loadBanner(it, container) }
        }
    }
}
