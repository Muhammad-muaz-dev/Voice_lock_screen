package com.futurecode.voicelockscreen.model



import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CommonAdsModel(
    @SerializedName("data")
    val data: Data?,
    @SerializedName("meta")
    val meta: Meta?
)

@Keep
data class Ads(
    @SerializedName("admob_app_open")
    val admobAppOpen: String?,
    @SerializedName("admob_banner")
    val admobBanner: String?,
    @SerializedName("admob_interstitial")
    val admobInterstitial: String?,
    @SerializedName("admob_native")
    val admobNative: String?,
    @SerializedName("admob_native_banner")
    val admobNativeBanner: String?,
    @SerializedName("admob_reward")
    val admobReward: String?,
    @SerializedName("ads_frequency")
    val adsFrequency: Int?,
    @SerializedName("ads_status")
    val adsStatus: String?,
    @SerializedName("custom_ads_status")
    val customAdsStatus: String?,
    @SerializedName("custom_url")
    val customUrl: List<String?>?,
    @SerializedName("inter_ads_flow")
    val interAdsFlow: List<String?>?,
    @SerializedName("meta_interstitial")
    val metaInterstitial: String?,
    @SerializedName("meta_native")
    val metaNative: String?,
    @SerializedName("meta_native_banner")
    val metaNativeBanner: String?,
    @SerializedName("native_ads_flow")
    val nativeAdsFlow: List<String?>?,
    @SerializedName("native_banner_ads_flow")
    val nativeBannerAdsFlow: List<String?>?,



    //changes
    @SerializedName("applovin_app_open")
    var applovin_app_open: String?,
    @SerializedName("applovin_banner")
    var applovin_banner: String?,
    @SerializedName("applovin_interstitial")
    var applovin_interstitial: String?,
    @SerializedName("applovin_mrec")
    var applovin_mrec: String?,
    @SerializedName("applovin_reward")
    var applovin_reward: String?,
    var app_open_ads_flow: List<String> = listOf("Applovin"),
    var banner_ads_flow:List<String> = listOf("Applovin"),
    var interstitial_ads_flow:List<String> = listOf("Applovin"),
    var mrec_ads_flow:List<String> = listOf("Applovin"),
    var reward_ads_flow: List<String> = listOf("Applovin"),
    var ad_network_index: Int=0,
    var native_ad_network_index: Int=0,
    var native_banner_ad_network_index: Int=0,
    var ad_show_counter:Int=0,
    var click_count: Int=0,
    var custom_urls: List<String> = emptyList(),
    var ad_frequency: Int=1,
    var ad_flow: List<String> =listOf("Admob"),
    var native_ad_flow: List<String> =listOf("Admob"),
    var native_banner_ad_flow: List<String> =listOf("Admob"),



)

@Keep
data class AppSettings(
    @SerializedName("contact_us")
    val contactUs: String?,
    @SerializedName("inappbanner_click_limit")
    val inappbannerClickLimit: Int?,
    @SerializedName("notification_url")
    val notificationUrl: String?,
    @SerializedName("notifications")
    val notifications: List<Notification?>?,
    @SerializedName("plan1_text")
    val plan1Text: String?,
    @SerializedName("plan2_text")
    val plan2Text: String?,
    @SerializedName("plan3_text")
    val plan3Text: String?,
    @SerializedName("privacy_policy")
    val privacyPolicy: String?,
    @SerializedName("promos")
    val promos: List<Promo?>?,
    @SerializedName("subscription_page")
    val subscriptionPage: String?,
    @SerializedName("inappbanner_url")
    val inappbannerUrl: String?,
    @SerializedName("terms_of_service")
    val termsOfService: String? = null
)

@Keep
data class Data(
    @SerializedName("ads")
    val ads: Ads?,
    @SerializedName("app_settings")
    val appSettings: AppSettings?
)

@Keep
data class Meta(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("status")
    val status: String?
)

@Keep
data class Notification(
    @SerializedName("banner")
    val banner: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("title")
    val title: String?
)


@Keep
data class Promo(
    @SerializedName("buttonText")
    val buttonText: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("image")
    val image: String?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("title")
    val title: String?
)
