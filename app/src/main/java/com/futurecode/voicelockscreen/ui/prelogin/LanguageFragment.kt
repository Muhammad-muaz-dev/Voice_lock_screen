package com.futurecode.voicelockscreen.ui.prelogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.adapter.LanguageAdapter
import com.futurecode.voicelockscreen.ads.AdPlacement
import com.futurecode.voicelockscreen.ads.AdViews
import com.futurecode.voicelockscreen.ads.AdsConfig
import com.futurecode.voicelockscreen.ads.AdsManager
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentLanguageBinding
import com.futurecode.voicelockscreen.model.LanguageOption

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {
    private lateinit var languageAdapter: LanguageAdapter
    private var selectedLanguageCode = "en"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val languages = languageOptions()
        val stored = savedInstanceState?.getString(STATE_SELECTION)
            ?: MyApplication.app.prefManager.selectedLanguage.ifBlank { "en" }
        val supportedCodes = languages.map { it.code }.toSet()
        selectedLanguageCode = stored.substringBefore('-').takeIf { it in supportedCodes } ?: "en"

        languageAdapter = LanguageAdapter(languages, selectedLanguageCode) { language ->
            selectedLanguageCode = language.code
            languageAdapter.updateSelection(language.code)
        }
        binding.rvLanguages.adapter = languageAdapter
        binding.btnContinue.setOnClickListener { confirmLanguage() }
        showNativeAd()
    }

    /** Native at the bottom, only in the launch flow (not when opened from Settings). */
    private fun showNativeAd() {
        AdViews.clearOnDetach(binding.languageAdContainer)
        if (isFromSettings()) return
        val config = AdsConfig.languageAds
        if (!AdsConfig.adsAllowed) return
        if (config.native) {
            // The native was started during splash; this hands it over (or waits for it).
            AdsManager.takeLanguageNative { ad ->
                val b = bindingOrNull
                when {
                    ad == null -> Unit
                    b == null -> ad.destroy()
                    else -> AdViews.showNative(b.languageAdContainer, ad, config)
                }
            }
        } else {
            AdPlacement.show(this, binding.languageAdContainer, config)
        }
    }

    private fun isFromSettings() = arguments?.getBoolean(ARG_RETURN_TO_SETTINGS, false) == true

    private fun confirmLanguage() {
        if (isFromSettings()) {
            findNavController().popBackStack()
        } else {
            // Navigate before applying the locale: the locale change recreates the activity and the
            // restored back stack keeps the next screen.
            PreloginFlow.continueFrom(this, PreloginFlow.Step.LANGUAGE)
        }
        MyApplication.applyLanguage(selectedLanguageCode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTION, selectedLanguageCode)
    }

    private fun languageOptions(): List<LanguageOption> {
        fun option(code: String, label: String, country: String) =
            LanguageOption(code, label, label, country)

        // Reference order first, then the remaining supported languages.
        return listOf(
            option("en", "English", "GB"),
            option("es", "Spanish", "ES"),
            option("pt", "Portuguese", "PT"),
            option("fr", "French", "FR"),
            option("de", "German", "DE"),
            option("it", "Italian", "IT"),
            option("ru", "Russian", "RU"),
            option("ar", "Arabic", "SA"),
            option("tr", "Turkish", "TR"),
            option("id", "Indonesian", "ID"),
            option("nl", "Dutch", "NL"),
            option("vi", "Vietnamese", "VN"),
            option("ja", "Japanese", "JP"),
            option("hi", "Hindi", "IN"),
            option("zh", "Chinese", "CN")
        )
    }

    companion object {
        const val ARG_RETURN_TO_SETTINGS = "return_to_settings"
        private const val STATE_SELECTION = "language_selection"
    }
}
