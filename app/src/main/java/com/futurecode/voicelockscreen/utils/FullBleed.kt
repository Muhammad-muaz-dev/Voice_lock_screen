package com.futurecode.voicelockscreen.utils

import android.app.Activity
import android.view.View
import androidx.core.view.ViewCompat
import com.futurecode.voicelockscreen.R

/**
 * The host activity root (`R.id.main`) fits system windows, which pads every screen away from
 * the status and navigation bars. Full-screen surfaces (lock screen preview, PIN lock test,
 * custom theme preview) switch that off so their wallpaper reaches the screen edges instead of
 * showing background bands at the top and bottom.
 */
object FullBleed {

    fun set(activity: Activity?, enabled: Boolean) {
        val root = activity?.findViewById<View>(R.id.main) ?: return
        if (root.fitsSystemWindows == !enabled) return
        root.fitsSystemWindows = !enabled
        if (enabled) root.setPadding(0, 0, 0, 0)
        ViewCompat.requestApplyInsets(root)
    }
}
