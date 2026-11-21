package com.futurecode.voicelockscreen.utils

import android.app.Activity
import android.app.Application
import android.app.Dialog
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides the system navigation bar in every screen of the app. A swipe from the bottom edge shows
 * it briefly; it hides again on its own. Screens keep their status bar.
 */
object SystemBars {

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                // Re-hide when focus returns (after dialogs, permission prompts, the keyboard, ads).
                activity.window.decorView.viewTreeObserver.addOnWindowFocusChangeListener(
                    ViewTreeObserver.OnWindowFocusChangeListener { hasFocus -> if (hasFocus) hide(activity.window) }
                )
            }

            override fun onActivityResumed(activity: Activity) = hide(activity.window)
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun hide(window: Window) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    /** Dialogs and bottom sheets have their own window; call this after show(). */
    fun hide(dialog: Dialog) {
        dialog.window?.let(::hide)
    }
}
