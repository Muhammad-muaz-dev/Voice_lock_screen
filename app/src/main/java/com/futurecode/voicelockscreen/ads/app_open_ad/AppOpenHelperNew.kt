package com.futurecode.voicelockscreen.ads.app_open_ad

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.FileUtils
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.futurecode.voicelockscreen.activity.MyApplication
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenHelperNew (private val configs: MyApplication): DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private var appOpenAd: AppOpenAd?=null
    private var loadCallback: AppOpenAd.AppOpenAdLoadCallback?=null
    private var currentActivity: Activity?=null
    private var loadTime: Long=0


    init {
        configs.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }


    private fun isUserEligibleForAds(): Boolean {
        val pref = MyApplication.app.prefManager
        // Dono false hone chahiye (na to ads off hon, na user Pro ho)
        return !pref.adsOff && !pref.isPro
    }

    override fun onStart(owner: LifecycleOwner) {
        showAdIfAvailable()
        super.onStart(owner)
    }

    private fun showAdIfAvailable() {

    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityResumed(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle
    ) {

    }

    override fun onActivityStarted(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }
}