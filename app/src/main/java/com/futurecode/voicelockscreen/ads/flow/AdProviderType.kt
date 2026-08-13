package com.futurecode.voicelockscreen.ads.flow

enum class AdProviderType(val apiName: String) {
    APPLOVIN("Applovin"),
    ADMOB("Admob"),
    META("Meta"),
    CUSTOM("Custom");

    companion object {
        fun from(value: String?): AdProviderType? {
            val normalized = value
                ?.trim()
                ?.trim('{', '}', '[', ']', '"', '\'')
                ?.replace("_", "")
                ?.replace("-", "")
                ?.replace(" ", "")
                ?.lowercase()
                ?: return null

            return when (normalized) {
                "applovin", "appolovin", "max" -> APPLOVIN
                "admob", "google", "googleadmob" -> ADMOB
                "meta", "facebook", "fan" -> META
                "custom", "mchamp" -> CUSTOM
                else -> null
            }
        }
    }
}