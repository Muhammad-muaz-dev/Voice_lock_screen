package com.futurecode.voicelockscreen.activity

import android.app.Activity
import android.app.Application
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.futurecode.voicelockscreen.ads.AdsManager
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.service.LockService
import com.futurecode.voicelockscreen.utils.SystemBars
import java.util.Locale


class MyApplication : Application() {

    lateinit var prefManager: PrefManager
    // Use nullable to avoid lateinit exceptions if ads are off
    // var appOpenHelper: AppOpenHelperNew? = null
   // private lateinit var networkMonitor: NetworkMonitor
    private var currentActivity: Activity? = null

    // private lateinit var analytics: FirebaseAnalytics


    override fun onCreate() {
        super.onCreate()
        app = this
        prefManager = PrefManager.get(this)
        // analytics = Firebase.analytics
        applyLanguage(prefManager.selectedLanguage, persistSelection = false)

        Log.d("TAG", "prefManagerOffffff: ${prefManager.adsOff}")

        // 1. Setup Activity Tracker FIRST
        // This is required because NetworkMonitor needs getCurrentActivity() to show the dialog
        setupActivityTracker()
        LockService.startIfNeeded(this)
        AdsManager.initialize(this)
        SystemBars.install(this)

        // 2. Initialize and Start Network Monitoring
//        networkMonitor = NetworkMonitor(this)
//        networkMonitor.startMonitoring()

        // 3. General Initializations
    }

    private fun setupActivityTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                currentActivity = activity
            }

            override fun onActivityStopped(activity: Activity) {
                if (currentActivity == activity) {
                    currentActivity = null
                }
            }

            override fun onActivityCreated(p0: Activity, p1: Bundle?) {}
            override fun onActivityResumed(p0: Activity) {}
            override fun onActivityPaused(p0: Activity) {}
            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}
            override fun onActivityDestroyed(p0: Activity) {}
        })
    }

    fun getCurrentActivity(): Activity? = currentActivity



    override fun onTerminate() {
        super.onTerminate()
//        if (::networkMonitor.isInitialized) {
//            networkMonitor.stopMonitoring()
//        }
    }

    companion object {
        lateinit var app: MyApplication

        fun applyLanguage(languageCode: String, persistSelection: Boolean = true) {
            val appLocales = LocaleListCompat.forLanguageTags(languageCode)

            if (persistSelection) {
                app.prefManager.selectedLanguage = languageCode
                app.prefManager.isLanguageSelectedFirstTime = true
            }

            Locale.setDefault(appLocales[0] ?: Resources.getSystem().configuration.locales[0])
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != appLocales.toLanguageTags()) {
                AppCompatDelegate.setApplicationLocales(appLocales)
            }
        }
    }
}
