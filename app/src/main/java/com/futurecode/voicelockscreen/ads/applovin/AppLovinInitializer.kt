package com.futurecode.voicelockscreen.ads.applovin


import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.applovin.sdk.AppLovinMediationProvider
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration

object AppLovinInitializer {
    private const val TAG = "AppLovinInitializer"
    private var isInitializing = false
    // private var APPLOVIN_SDK_KEY ="lXX44CuJahA5v9qZWTadHuSS_OGNpCia5XqG4FN_7E3Zfs7ORz4UiCDdbPWKflWm1bMMkrXK79BQu5WDUc5ROy"
    private var APPLOVIN_SDK_KEY ="lXX44CuJahA5v9qZWTadHuSS_OGNpCia5XqG4FN_7E3Zfs7ORz4UiCDdbPWKflWm1bMMkrXK79BQu5WDUc5ROy"

    fun initialize(context: Context, onInitialized: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        val sdk = AppLovinSdk.getInstance(appContext)
        if (sdk.isInitialized) {
            onInitialized(true)
            return
        }

        val sdkKey = APPLOVIN_SDK_KEY
        if (sdkKey.isBlank()) {
            Log.w(TAG, "AppLovin SDK key is missing. Add it to API as applovin_sdk_key.")
            onInitialized(false)
            return
        }

        if (isInitializing) {
            Handler(Looper.getMainLooper()).postDelayed({
                initialize(appContext, onInitialized)
            }, 500)
            return
        }

        isInitializing = true
        val initConfig = AppLovinSdkInitializationConfiguration.builder(sdkKey)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()

        sdk.initialize(initConfig) {
            isInitializing = false
            onInitialized(true)
        }
    }
}