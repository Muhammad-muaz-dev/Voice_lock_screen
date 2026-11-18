package com.futurecode.voicelockscreen.utils

import android.content.Context
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.annotation.ColorInt
import androidx.core.view.WindowCompat
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.google.gson.Gson
import java.util.Calendar


//fun getNotificationListFromPrefs(): List<NotificationModel> {
//    return try {
//        val jsonString = MyApplication.app.prefManager.notificationList
//
//        if (jsonString.isNullOrEmpty()) return emptyList()
//
//        Gson().fromJson(
//            jsonString,
//            Array<NotificationModel>::class.java
//        )?.toList() ?: emptyList()
//
//    } catch (e: Exception) {
//        e.printStackTrace()
//        emptyList()
//    }
//}

/**
 * Status bar ka background color set karta hai aur text/icons (Time, Battery, Network)
 * ko pure WHITE render karta hai (Jaise screenshot me hai).
 *
 * @param backgroundColor Status bar ka background color (e.g., Color.parseColor("#121B22") ya R.color.dark_bg)
 */
fun Window.setDarkStatusBarWithWhiteIcons(@ColorInt backgroundColor: Int) {
    // 1. Status bar background color apply karein
    statusBarColor = backgroundColor

    // 2. Version-wise Icons & Text styling
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // --- Android 11 (API 30) & Above ---
        // Ensure standard window fitting so layout stays below status bar
        WindowCompat.setDecorFitsSystemWindows(this, true)

        // Clear APPEARANCE_LIGHT_STATUS_BARS flag -> Makes text/icons WHITE
        insetsController?.setSystemBarsAppearance(
            0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        // --- Android 6.0 (API 23) to Android 10 (API 29) ---
        @Suppress("DEPRECATION")
        var flags = decorView.systemUiVisibility

        // Remove LIGHT_STATUS_BAR flag so icons turn WHITE
        @Suppress("DEPRECATION")
        flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()

        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = flags
    } else {
        // --- Android 5.0 (API 21) & 5.1 (API 22) ---
        // Pre-Marshmallow natively renders status bar icons in WHITE by default
        @Suppress("DEPRECATION")
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }
}


/** Light-scheme screens: light status bar background with dark icons. */
fun Window.setLightStatusBarWithDarkIcons(@ColorInt backgroundColor: Int) {
    statusBarColor = backgroundColor
    WindowCompat.setDecorFitsSystemWindows(this, true)
    WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = true
}


fun Context.getTimeBasedGreetingRes(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val stringResId = when (hour) {
        in 5..11 -> R.string.good_morning
        in 12..16 -> R.string.good_afternoon
        in 17..20 -> R.string.good_evening
        else -> R.string.good_night
    }
    return getString(stringResId)
}

