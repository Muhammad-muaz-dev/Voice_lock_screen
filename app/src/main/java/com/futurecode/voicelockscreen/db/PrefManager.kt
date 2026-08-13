package com.futurecode.voicelockscreen.db

import android.content.Context
import com.futurecode.voicelockscreen.utils.AppConstants
import com.futurecode.voicelockscreen.utils.KeyConstants

class PrefManager(private val context: Context) {

    var nativeAdFlow: String
        get() = sharedPref.getString(AppConstants.NATIVE_AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.NATIVE_AD_FLOW.name, value).apply()
        }

    var nativeBannerAdFlow: String
        get() = sharedPref.getString(AppConstants.NATIVE_BANNER_AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.NATIVE_BANNER_AD_FLOW.name, value).apply()
        }

    var adsOff: Boolean
        get() = sharedPref.getBoolean(AppConstants.ADS_OFF.name, false)
        set(value) {
            editor.putBoolean(AppConstants.ADS_OFF.name, value).apply()
        }

    var customOff: Boolean
        get() = sharedPref.getBoolean(AppConstants.CUSTOM_OFF.name, false)
        set(value) {
            editor.putBoolean(AppConstants.CUSTOM_OFF.name, value).apply()
        }

    var shareMessage: String
        get() = sharedPref.getString(AppConstants.SHARE_MESSAGE.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.SHARE_MESSAGE.name, value).apply()
        }

    private val sharedPref = context.getSharedPreferences(KeyConstants.PREF_NAME, Context.MODE_PRIVATE)
    private val editor = sharedPref.edit()


    var isOnboardingDone: Boolean
        get() = sharedPref.getBoolean(AppConstants.KEY_ONBOARDING_DONE.name,false)
        set(value){
            editor.putBoolean(AppConstants.KEY_ONBOARDING_DONE.name,value).apply()
        }

    var selectedLanguage: String
        get() = sharedPref.getString(AppConstants.KEY_LANGAUGE.name, DEFAULT_LANGUAGE)
            .orEmpty()
            .ifBlank { DEFAULT_LANGUAGE }
        set(value) {
            sharedPref.edit()
                .putString(AppConstants.KEY_LANGAUGE.name, value.ifBlank { DEFAULT_LANGUAGE })
                .apply()
        }

    var isLanguageSelectedFirstTime: Boolean
        get() = sharedPref.getBoolean(KEY_LANGUAGE_SELECTED, false)
        set(value) {
            sharedPref.edit()
                .putBoolean(KEY_LANGUAGE_SELECTED, value)
                .apply()
        }

    var isNotificationStarts: Boolean
        get() = sharedPref.getBoolean(AppConstants.IS_NOTIFICATION_STARTS.name, false)
        set(value) {
            editor.putBoolean(AppConstants.IS_NOTIFICATION_STARTS.name, value).apply()
        }


    var isPro: Boolean
        get() = sharedPref.getBoolean(AppConstants.IS_PRO.name, false)
        set(value) {
            editor.putBoolean(AppConstants.IS_PRO.name, value).apply()
        }

    var accountEmail: String
        get() = sharedPref.getString(AppConstants.ACCOUNT_EMAIL.name, "").orEmpty()
        set(value) {
            editor.putString(AppConstants.ACCOUNT_EMAIL.name, value).apply()
        }

    var accountPassword: String
        get() = sharedPref.getString(AppConstants.ACCOUNT_PASSWORD.name, "").orEmpty()
        set(value) {
            editor.putString(AppConstants.ACCOUNT_PASSWORD.name, value).apply()
        }

    var isAccountLoggedIn: Boolean
        get() = sharedPref.getBoolean(AppConstants.IS_ACCOUNT_LOGGED_IN.name, false)
        set(value) {
            editor.putBoolean(AppConstants.IS_ACCOUNT_LOGGED_IN.name, value).apply()
        }


    fun getNextNotificationIndex(size: Int): Int {
        val currentIndex = sharedPref.getInt(AppConstants.NOTIFICATION_KEY_INDEX.name, 0)
        val nextIndex = (currentIndex + 1) % size

        editor.putInt(AppConstants.NOTIFICATION_KEY_INDEX.name, nextIndex).apply()
        return currentIndex
    }
    var notificationUrl: String
        get() = sharedPref.getString(AppConstants.NOTIFICATION_URL.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.NOTIFICATION_URL.name, value).apply()
        }

    var admobBanner: String
        get() = sharedPref.getString(AppConstants.ADMOB_BANNER.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.ADMOB_BANNER.name, value).apply()
        }

    var admobInterstitial: String
        get() = sharedPref.getString(AppConstants.ADMOB_INTERSTITIAL.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.ADMOB_INTERSTITIAL.name, value).apply()
        }

    var admobNative: String
        get() = sharedPref.getString(AppConstants.ADMOB_NATIVE.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.ADMOB_NATIVE.name, value).apply()
        }

    var admobRewardAd: String
        get() = sharedPref.getString(AppConstants.ADMOB_REWARD_AD.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.ADMOB_REWARD_AD.name, value).apply()
        }

    var admobAppOpen: String
        get() = sharedPref.getString(AppConstants.ADMOB_APP_OPEN.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.ADMOB_APP_OPEN.name, value).apply()
        }


//    var applovin_app_open: String
//        get() = sharedPref.getString(AppConstants.APPLOVIN_APP_OPEN.name, "") ?: ""
//        set(value) {
//            editor.putString(AppConstants.APPLOVIN_APP_OPEN.name, value).apply()
//        }
    //  APPLOVIN_APP_OPEN

    var metaInterstitial: String
        get() = sharedPref.getString(AppConstants.META_INTERSTITIAL.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.META_INTERSTITIAL.name, value).apply()
        }

    var metaNative: String
        get() = sharedPref.getString(AppConstants.META_NATIVE.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.META_NATIVE.name, value).apply()
        }

    var promosList: String
        get() = sharedPref.getString(AppConstants.PromosList.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.PromosList.name, value).apply()
        }

    var clickLimit: Int
        get() = sharedPref.getInt(AppConstants.CLICK_LIMIT.name, 0)
        set(value) {
            editor.putInt(AppConstants.CLICK_LIMIT.name, value).apply()
        }

    var clickUrl: String
        get() = sharedPref.getString(AppConstants.CLICK_URL.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.CLICK_URL.name, value).apply()
        }

    var clickCount: Int
        get() = sharedPref.getInt(AppConstants.CLICK_COUNT.name, 0)
        set(value) {
            editor.putInt(AppConstants.CLICK_COUNT.name, value).apply()
        }

    var promoIndex: Int
        get() = sharedPref.getInt(AppConstants.PROMO_INDEX.name, 0)
        set(value) {
            editor.putInt(AppConstants.PROMO_INDEX.name, value).apply()
        }

    var adShowCounter: Int
        get() = sharedPref.getInt(AppConstants.AD_SHOW_COUNTER.name, 0)
        set(value) {
            editor.putInt(AppConstants.AD_SHOW_COUNTER.name, value).apply()
        }

    var adNetworkIndex: Int
        get() = sharedPref.getInt(AppConstants.AD_NETWORK_INDEX.name, 0)
        set(value) {
            editor.putInt(AppConstants.AD_NETWORK_INDEX.name, value).apply()
        }


    var notificationList: String
        get() = sharedPref.getString(AppConstants.NotificationsList.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.NotificationsList.name, value).apply()
        }
    var nativeAdNetworkIndex: Int
        get() = sharedPref.getInt(AppConstants.NATIVE_AD_NETWORK_INDEX.name, 0)
        set(value) {
            editor.putInt(AppConstants.NATIVE_AD_NETWORK_INDEX.name, value).apply()
        }

    var nativeBannerAdNetworkIndex: Int
        get() = sharedPref.getInt(AppConstants.NATIVE_BANNER_AD_NETWORK_INDEX.name, 0)
        set(value) {
            editor.putInt(AppConstants.NATIVE_BANNER_AD_NETWORK_INDEX.name, value).apply()
        }

    var customUrls: String
        get() = sharedPref.getString(AppConstants.CUSTOM_URLS.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.CUSTOM_URLS.name, value).apply()
        }

    var adFrequency: Int
        get() = sharedPref.getInt(AppConstants.AD_FREQUENCY.name, 0)
        set(value) {
            editor.putInt(AppConstants.AD_FREQUENCY.name, value).apply()
        }

    var adFlow: String
        get() = sharedPref.getString(AppConstants.AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.AD_FLOW.name, value).apply()
        }

    var contactUs: String
        get() = sharedPref.getString(AppConstants.CONTACT_US.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.CONTACT_US.name, value).apply()
        }

    var privacyPolicy: String
        get() = sharedPref.getString(AppConstants.PRIVACY_POLICY.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.PRIVACY_POLICY.name, value).apply()
        }

    var subscriptionPage: String
        get() = sharedPref.getString(AppConstants.SUBSCRIPTION_PAGE.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.SUBSCRIPTION_PAGE.name, value).apply()
        }


    var weekly: String
        get() = sharedPref.getString(AppConstants.WEEKLY.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.WEEKLY.name, value).apply()
        }

    var monthly: String
        get() = sharedPref.getString(AppConstants.MONTHLY.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.MONTHLY.name, value).apply()
        }



    var quarterly: String
        get() = sharedPref.getString(AppConstants.QUARTERLY.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.QUARTERLY.name, value).apply()
        }


    var appLovinBanner: String
        get() = sharedPref.getString(AppConstants.APPLOVIN_BANNER.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.APPLOVIN_BANNER.name, value).apply()
        }

    var appOpenAdFlow: String
        get() = sharedPref.getString(AppConstants.APP_OPEN_AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.APP_OPEN_AD_FLOW.name, value).apply()
        }

    var bannerAdFlow: String
        get() = sharedPref.getString(AppConstants.BANNER_AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.BANNER_AD_FLOW.name, value).apply()
        }


    var interstitialAdsFlow: String
        get() = sharedPref.getString(AppConstants.INTERSTITIAL_AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.INTERSTITIAL_AD_FLOW.name, value).apply()
        }


    var mrecAdsFlow: String
        get() = sharedPref.getString(AppConstants.MREC_AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.MREC_AD_FLOW.name, value).apply()
        }


    var applovinMrec: String
        get() = sharedPref.getString(AppConstants.APPLOVIN_MREC.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.APPLOVIN_MREC.name, value).apply()
        }




    var metaBanner: String
        get() = sharedPref.getString(AppConstants.META_BANNER.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.META_BANNER.name, value).apply()
        }


    var admobNativeBanner: String
        get() = sharedPref.getString(AppConstants.ADMOB_NATIVE_BANNER.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.ADMOB_NATIVE_BANNER.name, value).apply()
        }

    var applovin_app_open: String
        get() = sharedPref.getString(AppConstants.APPLOVIN_APP_OPEN.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.APPLOVIN_APP_OPEN.name, value).apply()
        }

    var applovinInterstitial: String
        get() = sharedPref.getString(AppConstants.APPLOVIN_INTERSTITIAL.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.APPLOVIN_INTERSTITIAL.name, value).apply()
        }


    var rewardAdsFlow: String
        get() = sharedPref.getString(AppConstants.REWARD_AD_FLOW.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.REWARD_AD_FLOW.name, value).apply()
        }


    var applovinReward: String
        get() = sharedPref.getString(AppConstants.APPLOVIN_REWARD.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.APPLOVIN_REWARD.name, value).apply()
        }


    var metaNativeBanner: String
        get() = sharedPref.getString(AppConstants.META_NATIVE_BANNER.name, "") ?: ""
        set(value) {
            editor.putString(AppConstants.META_NATIVE_BANNER.name, value).apply()
        }

    fun clearPreferences() {
        editor.clear().apply()
    }

    fun clearParticularKey(value: String) {
        editor.remove(value).apply()
    }

    companion object {
        private const val DEFAULT_LANGUAGE = "en"
        private const val KEY_LANGUAGE_SELECTED = "IS_LANGUAGE_SELECTED"

        fun get(context: Context) = PrefManager(context)
    }
}
