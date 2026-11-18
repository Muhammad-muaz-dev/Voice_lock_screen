package com.futurecode.voicelockscreen.utils


import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.db.PrefManager

import com.futurecode.voicelockscreen.model.Promo
import com.google.gson.Gson
import org.json.JSONArray
import java.lang.reflect.Field
import java.util.Locale
import java.util.Random

object Utils {
    fun Float.dpToPx(activity: Activity): Float =
        this * activity.resources.displayMetrics.density

    fun NumberPicker.removeDividers() {
        try {
            val fields: Array<Field> = NumberPicker::class.java.declaredFields
            for (field in fields) {
                if (field.name == "mSelectionDivider") {
                    field.isAccessible = true
                    field.set(this, null)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SoonBlockedPrivateApi")
    fun NumberPicker.applySelectedColor(hexColor: String) {
        try {
            val selectorWheelPaintField =
                NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            selectorWheelPaintField.isAccessible = true
            (selectorWheelPaintField.get(this) as android.graphics.Paint).apply {
                color = Color.parseColor(hexColor)
                textSize = 52f * resources.displayMetrics.scaledDensity
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.setFormatter { value -> value.toString() }
    }

    fun getWidth(context: Context): Int {
        val displayMetrics = DisplayMetrics()
        val windowmanager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowmanager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    fun formatTimestampToHours(timestamp: Long): String {
        val diffMinutes = (System.currentTimeMillis() - timestamp) / (1000 * 60)
        return when {
            diffMinutes < 60 -> MyApplication.app.getString(R.string.just_now)
            else -> {
                val hours = diffMinutes / 60
                MyApplication.app.getString(R.string.hours_short, hours)
            }
        }
    }

    fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) return true
        }
        return false
    }

    fun openCustomChrome(activity: Activity, url: String) {
        openBrowser(activity, url)
    }

    fun openBrowser(activity: Activity, url: String) {
        val link = url.ifBlank { "https://www.google.com" }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            activity.startActivity(intent)
        } catch (ex: Exception) {
            Toast.makeText(activity, R.string.no_browser_found, Toast.LENGTH_LONG).show()
        }
    }

    fun jsonToStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            list.add(array.getString(i))
        }
        return list
    }

    fun getRandomUrls(context: Context): String {
        val list = jsonToStringList(PrefManager.get(context).customUrls)
        return if (list.isNotEmpty()) {
            list[Random().nextInt(list.size)]
        } else {
            "https://www.google.com/"
        }
    }



    /**
     * Corrected method: Initializes PrefManager using the local context
     * to avoid a NullPointerException on app startup.
     */
    fun updateBaseContextLocale(context: Context): Context {
        val pref = PrefManager.get(context)
        val languageCode = pref.selectedLanguage
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        config.fontScale = 1.0f

        // Use deprecated updateConfiguration for broader resource coverage
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        return context.createConfigurationContext(config)
    }

    fun getPromosListFromPrefs(): List<Promo> {
        return try {
            val jsonString = MyApplication.app.prefManager.promosList

            if (jsonString.isNullOrEmpty()) return emptyList()

            Gson().fromJson(
                jsonString,
                Array<Promo>::class.java
            )?.toList() ?: emptyList()

        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
