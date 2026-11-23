package com.futurecode.voicelockscreen.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.core.view.OneShotPreDrawListener
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.futurecode.voicelockscreen.R

/** Covers the activity while a preloaded full-screen ad is handed to the SDK. */
internal class FullscreenAdTransition(
    private val activity: Activity,
    private val onFinished: () -> Unit
) {
    private val main = Handler(Looper.getMainLooper())
    private val lifecycle = (activity as? LifecycleOwner)?.lifecycle
    private var overlay: View? = null
    private var preDraw: OneShotPreDrawListener? = null
    private var launch: Runnable? = null
    private var showRequested = false
    private var finished = false
    private val timeout = Runnable { finish() }
    private val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> if (!showRequested) finish() else hideLoader()
            Lifecycle.Event.ON_DESTROY -> finish()
            else -> Unit
        }
    }

    fun show(showAd: () -> Unit) {
        if (!canShow()) {
            finish()
            return
        }
        val content = activity.findViewById<ViewGroup>(android.R.id.content)
        val loading = activity.layoutInflater.inflate(R.layout.view_fullscreen_ad_loading, content, false)
        overlay = loading
        content.addView(loading)
        lifecycle?.addObserver(observer)
        main.postDelayed(timeout, START_TIMEOUT_MS)
        preDraw = loading.doOnPreDraw {
            val start = Runnable {
                if (!canShow()) {
                    finish()
                } else {
                    showRequested = true
                    runCatching(showAd).onFailure { finish() }
                }
            }
            launch = start
            main.postDelayed(start, MIN_VISIBLE_MS)
        }
    }

    fun onAdOpened() {
        if (finished) return
        main.removeCallbacks(timeout)
        hideLoader()
    }

    fun finish() {
        if (finished) return
        finished = true
        main.removeCallbacks(timeout)
        launch?.let(main::removeCallbacks)
        lifecycle?.removeObserver(observer)
        hideLoader()
        onFinished()
    }

    private fun hideLoader() {
        preDraw?.removeListener()
        preDraw = null
        val loading = overlay ?: return
        overlay = null
        (loading.parent as? ViewGroup)?.removeView(loading)
    }

    private fun canShow() = !activity.isFinishing && !activity.isDestroyed && AdsConfig.adsAllowed &&
        lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false

    private companion object {
        const val MIN_VISIBLE_MS = 350L
        const val START_TIMEOUT_MS = 5_000L
    }
}
