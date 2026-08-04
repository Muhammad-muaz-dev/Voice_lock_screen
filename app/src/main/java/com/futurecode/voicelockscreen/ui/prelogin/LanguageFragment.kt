package com.futurecode.voicelockscreen.ui.prelogin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.adapter.LanguageAdapter
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentLanguageBinding
import com.futurecode.voicelockscreen.model.LanguageOption

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private val languages = listOf(
        LanguageOption("en", "English (US)", "English"),
        LanguageOption("es", "Spanish", "Español"),
        LanguageOption("fr", "French", "Français"),
        LanguageOption("de", "German", "Deutsch"),
        LanguageOption("ja", "Japanese", "日本語"),
        LanguageOption("hi", "Hindi", "हिन्दी"),
        LanguageOption("pt", "Portuguese", "Português"),
        LanguageOption("ru", "Russian", "Русский"),
        LanguageOption("zh", "Chinese", "中文"),
        LanguageOption("ar", "Arabic", "العربية")
    )

    private lateinit var languageAdapter: LanguageAdapter
    private var selectedLanguageCode = "en"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedLanguageCode = MyApplication.app.prefManager.selectedLanguage.ifBlank { "en" }
        languageAdapter = LanguageAdapter(requireActivity(), buildLanguageItems(languages), selectedLanguageCode) { language ->
            selectLanguage(language)
        }

        binding.rvLanguages.adapter = languageAdapter
        languageAdapter.submitList(buildLanguageItems(languages))

        binding.etSearchLanguage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLanguages(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.btnContinue.setOnClickListener {
            MyApplication.applyLanguage(selectedLanguageCode)
            if (arguments?.getBoolean(ARG_RETURN_TO_SETTINGS, false) == true) {
                findNavController().popBackStack()
            } else {
                findNavController().navigate(R.id.action_languageFragment_to_onBoardingFragment)
            }
        }
    }

    private fun selectLanguage(language: LanguageOption) {
        selectedLanguageCode = language.code
        MyApplication.applyLanguage(language.code)
        languageAdapter.updateSelection(language.code)
    }

    private fun filterLanguages(query: String) {
        val normalizedQuery = query.trim()
        val filteredLanguages = if (normalizedQuery.isEmpty()) {
            languages
        } else {
            languages.filter { language ->
                language.name.contains(normalizedQuery, ignoreCase = true) ||
                    language.nativeName.contains(normalizedQuery, ignoreCase = true)
            }
        }

        languageAdapter.submitList(buildLanguageItems(filteredLanguages))
        binding.tvEmptyLanguages.isVisible = filteredLanguages.isEmpty()
    }

    private fun buildLanguageItems(source: List<LanguageOption>): List<Any> {
        if (source.isEmpty()) return emptyList()

        val items = mutableListOf<Any>()
        source.forEachIndexed { index, language ->
            items.add(language)
            if (index == AD_INSERT_AFTER_INDEX) {
                items.add(AD_UNIT)
            }
        }
        if (source.size <= AD_INSERT_AFTER_INDEX) {
            items.add(AD_UNIT)
        }
        return items
    }



    companion object {
        const val ARG_RETURN_TO_SETTINGS = "return_to_settings"
        const val AD_UNIT = "AD_UNIT"
        private const val AD_INSERT_AFTER_INDEX = 1
    }
}
