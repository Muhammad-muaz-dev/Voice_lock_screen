package com.futurecode.voicelockscreen.ads.native_ads_banner

import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.utils.Utils


object NativeBannerAdsLogic {

    private val pref: PrefManager = MyApplication.app.prefManager

    fun getCurrentNativeBannerNetwork(): String {
        return try {
            val adNetworks = Utils.jsonToStringList(pref.nativeBannerAdFlow)

            if (adNetworks.isEmpty()) {
                pref.nativeAdNetworkIndex = 0
                return "Admob"
            }

            var index = pref.nativeAdNetworkIndex
            if (index !in adNetworks.indices) index = 0

            val currentNetwork = adNetworks[index]
            pref.nativeAdNetworkIndex = (index + 1) % adNetworks.size

            currentNetwork
        } catch (e: Exception) {
            e.printStackTrace()
            pref.nativeAdNetworkIndex = 0
            "Admob"
        }
    }
}
