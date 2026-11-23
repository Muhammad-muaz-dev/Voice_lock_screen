package com.futurecode.voicelockscreen.ads

import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.futurecode.voicelockscreen.R

/** Fills a screen's bottom ad slot from its JSON: native when NativeEnable, else banner when BannerEnable. */
object AdPlacement {

    /** Loads the shared native ad and returns a cleanup action for recyclable placements. */
    @MainThread
    fun loadAndShowNative(owner: LifecycleOwner, container: ViewGroup): () -> Unit {
        val lifecycle = owner.lifecycle
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) return {}
        AdViews.clear(container)
        if (!AdsConfig.adsAllowed) return {}

        val requestToken = Any()
        container.tag = requestToken
        var placementTag: Any = requestToken
        val cleanup = object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                if (container.tag === placementTag) AdViews.clear(container)
                owner.lifecycle.removeObserver(this)
            }
        }
        lifecycle.addObserver(cleanup)

        AdsManager.loadNative(AdIds.native_all) { ad ->
            if (container.tag !== requestToken || ad == null || !AdsConfig.adsAllowed) {
                ad?.destroy()
                if (container.tag === requestToken) AdViews.clear(container)
                lifecycle.removeObserver(cleanup)
                return@loadNative
            }

            AdViews.showNative(container, ad, R.layout.layout_native_ad_onboarding)
            placementTag = ad
        }

        return { cleanup.onDestroy(owner) }
    }

    fun show(
        fragment: Fragment,
        container: ViewGroup,
        config: ScreenAdConfig,
        @LayoutRes mediaLayout: Int = R.layout.layout_native_ad_media
    ) {
        if (!AdsConfig.adsAllowed) return
        when {
            config.native -> AdsManager.loadNative { ad ->
                val alive = fragment.view != null && container.isAttachedToWindow
                when {
                    ad == null -> Unit
                    !alive -> ad.destroy()
                    else -> AdViews.showNative(container, ad, config, mediaLayout)
                }
            }
            config.banner -> fragment.activity?.let { AdViews.loadBanner(it, container) }
        }
    }
}
