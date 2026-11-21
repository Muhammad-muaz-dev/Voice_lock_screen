package com.futurecode.voicelockscreen.ads

import android.graphics.Color
import org.json.JSONObject

/**
 * One screen's ad settings, parsed from a Remote Config JSON such as:
 * {"AppOpenEnable":0,"InterstitialEnable":1,"BannerEnable":1,"NativeEnable":0,
 *  "SplashConsentTimeout":7000,"NativeFormat":2,"AdCtaColor":"#A167FE",
 *  "AdCtaTextColor":"#FFFFFF","CtaTextStyle":"bold"}
 * Missing or invalid fields fall back to the screen's defaults.
 */
data class ScreenAdConfig(
    val appOpen: Boolean,
    val interstitial: Boolean,
    val banner: Boolean,
    val native: Boolean,
    /** Longest the splash waits for its full-screen ad, in ms. */
    val timeoutMs: Long,
    /** 1 = compact native (no media), 2 = large native with media. */
    val nativeFormat: Int,
    val ctaColor: Int,
    val ctaTextColor: Int,
    /** "bold", "semibold", "medium" or "normal". */
    val ctaTextStyle: String
) {
    val nativeHasMedia: Boolean get() = nativeFormat >= FORMAT_MEDIA

    companion object {
        const val FORMAT_COMPACT = 1
        const val FORMAT_MEDIA = 2

        fun parse(json: String, defaults: ScreenAdConfig): ScreenAdConfig {
            val obj = runCatching { JSONObject(json) }.getOrNull() ?: return defaults
            fun flag(key: String, fallback: Boolean) =
                if (obj.has(key)) obj.optInt(key, if (fallback) 1 else 0) == 1 else fallback
            fun color(key: String, fallback: Int) =
                obj.optString(key).takeIf { it.isNotBlank() }?.let { runCatching { Color.parseColor(it) }.getOrNull() } ?: fallback
            return ScreenAdConfig(
                appOpen = flag("AppOpenEnable", defaults.appOpen),
                interstitial = flag("InterstitialEnable", defaults.interstitial),
                banner = flag("BannerEnable", defaults.banner),
                native = flag("NativeEnable", defaults.native),
                timeoutMs = obj.optLong("SplashConsentTimeout", defaults.timeoutMs).coerceIn(1000, 20000),
                nativeFormat = obj.optInt("NativeFormat", defaults.nativeFormat),
                ctaColor = color("AdCtaColor", defaults.ctaColor),
                ctaTextColor = color("AdCtaTextColor", defaults.ctaTextColor),
                ctaTextStyle = obj.optString("CtaTextStyle").ifBlank { defaults.ctaTextStyle }.lowercase()
            )
        }

        /** Same shape as the Remote Config value, for the in-app defaults. */
        fun toJson(c: ScreenAdConfig): String = JSONObject()
            .put("AppOpenEnable", if (c.appOpen) 1 else 0)
            .put("InterstitialEnable", if (c.interstitial) 1 else 0)
            .put("BannerEnable", if (c.banner) 1 else 0)
            .put("NativeEnable", if (c.native) 1 else 0)
            .put("SplashConsentTimeout", c.timeoutMs)
            .put("NativeFormat", c.nativeFormat)
            .put("AdCtaColor", String.format("#%06X", 0xFFFFFF and c.ctaColor))
            .put("AdCtaTextColor", String.format("#%06X", 0xFFFFFF and c.ctaTextColor))
            .put("CtaTextStyle", c.ctaTextStyle)
            .toString()
    }
}
