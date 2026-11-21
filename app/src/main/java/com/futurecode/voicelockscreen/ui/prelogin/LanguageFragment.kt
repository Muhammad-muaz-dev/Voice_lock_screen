package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.adapter.LanguageAdapter
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
        binding.btnContinue.setOnClickListener {
            MyApplication.applyLanguage(selectedLanguageCode)
            if (arguments?.getBoolean(ARG_RETURN_TO_SETTINGS, false) == true) {
                findNavController().popBackStack()
            } else {
                goToHome()
            }
        }
    }

    private fun goToHome() {
        MyApplication.app.prefManager.isOnboardingDone = true
        val intent = Intent(requireContext(), HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(HomeActivity.EXTRA_SHOW_PREMIUM, true)
        }
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SELECTION, selectedLanguageCode)
    }

    private fun languageOptions(): List<LanguageOption> {
        fun option(code: String, label: String, country: String) =
            LanguageOption(code, label, label, country)

        return listOf(
            option("en", "English", "GB"),
            option("es", "Español", "ES"),
            option("id", "Bahasa Indonesia", "ID"),
            option("de", "Deutsch", "DE"),
            option("fr", "Français", "FR"),
            option("it", "Italiano", "IT"),
            option("nl", "Nederlands", "NL"),
            option("pt", "Português", "PT"),
            option("vi", "Tiếng Việt", "VN"),
            option("tr", "Türkçe", "TR"),
            option("ru", "Русский", "RU"),
            option("ja", "日本語", "JP"),
            option("hi", "हिन्दी", "IN"),
            option("zh", "中文", "CN"),
            option("ar", "العربية", "SA")
        )
    }

    companion object {
        const val ARG_RETURN_TO_SETTINGS = "return_to_settings"
        private const val STATE_SELECTION = "language_selection"
    }
}
