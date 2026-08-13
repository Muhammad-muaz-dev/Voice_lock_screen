package com.futurecode.voicelockscreen.ads.flow

fun interface AdProviderExecutor {
    fun execute(providerType: AdProviderType, onSuccess: () -> Unit, onFailure: (String) -> Unit)
}