package com.futurecode.voicelockscreen.ads.flow


import android.content.Context
import android.util.Log
import kotlin.collections.get
import kotlin.io.resolve

object AdWaterfallManager {
    private const val TAG = "AdWaterfallManager"

    fun run(
        context: Context,
        flowKey: String,
        placementName: String,
        fallback: List<AdProviderType> = listOf(AdProviderType.ADMOB, AdProviderType.CUSTOM),
        executor: AdProviderExecutor,
        onAllFailed: (String) -> Unit
    ) {
        val providers = AdFlowResolver.resolve(context, flowKey, fallback)
        var index = 0

        fun tryNext(lastError: String? = null) {
            if (index >= providers.size) {
                onAllFailed(lastError ?: "No provider loaded for $placementName")
                return
            }

            val provider = providers[index]
            val providerIndex = index
            index += 1

            Log.d(TAG, "$placementName provider[$providerIndex]=${provider.apiName}")
            executor.execute(
                provider,
                {
                    Log.d(TAG, "$placementName loaded with ${provider.apiName}")
                },
                { error ->
                    Log.d(TAG, "$placementName failed with ${provider.apiName}: $error")
                    tryNext(error)
                }
            )
        }

        tryNext()
    }
}