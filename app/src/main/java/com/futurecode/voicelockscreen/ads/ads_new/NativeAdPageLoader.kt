package com.futurecode.voicelockscreen.ads.ads_new

import com.futurecode.voicelockscreen.databinding.ItemOnboardingNativeAdBinding

interface NativeAdPageLoader {
    fun load(binding: ItemOnboardingNativeAdBinding)

    fun destroy() = Unit
}
