package com.futurecode.voicelockscreen.ads.flow


import android.content.Context
import android.util.Log
import com.futurecode.voicelockscreen.db.PrefManager
import com.futurecode.voicelockscreen.utils.Utils

object AdFlowResolver {
    private const val TAG = "AdFlowResolver"

    fun resolve(
        context: Context,
        flowKey: String,
        fallback: List<AdProviderType> = listOf(AdProviderType.ADMOB, AdProviderType.CUSTOM)
    ): List<AdProviderType> {
        val rawFlow = runCatching {
            Utils.jsonToStringList(flowKey)
        }.getOrElse {
            Log.d(TAG, "Unable to parse flow $flowKey: ${it.localizedMessage}")
            listOf(flowKey)
        }

        val providers = rawFlow
            .flatMap { expandListValue(it) }
            .mapNotNull { AdProviderType.from(it) }

        return providers.ifEmpty { fallback }
    }

    private fun expandListValue(value: String): List<String> {
        val trimmed = value.trim()
        val unwrapped = trimmed.trim('{', '}', '[', ']')
        return if (unwrapped.contains(",")) {
            unwrapped.split(",").map { it.trim() }
        } else {
            listOf(trimmed)
        }
    }
}
