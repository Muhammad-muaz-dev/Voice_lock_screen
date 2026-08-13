package com.futurecode.voicelockscreen.ads


import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.futurecode.voicelockscreen.R


class LoaderAds(activity: Activity, cancelable: Boolean) {
    private val alertDialog: AlertDialog?

    init {
        val dialogBuilder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.progress_dialog, null)
        dialogBuilder.setView(dialogView)
        alertDialog = dialogBuilder.create()
        alertDialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.setCancelable(cancelable)
    }

    fun show() {
        if (alertDialog != null && !alertDialog.isShowing) alertDialog.show()
    }

    fun dismiss() {
        if (alertDialog != null && alertDialog.isShowing) alertDialog.dismiss()
    }

    val isShowing: Boolean
        get() = if (alertDialog != null && alertDialog.isShowing) {
            true
        } else {
            false
        }
}