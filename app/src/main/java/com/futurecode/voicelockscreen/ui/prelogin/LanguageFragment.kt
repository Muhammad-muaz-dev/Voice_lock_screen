package com.futurecode.voicelockscreen.ui.prelogin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentLanguageBinding
import com.futurecode.voicelockscreen.databinding.ItemLanguageBinding

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
        languageAdapter = LanguageAdapter(selectedLanguageCode) { language ->
            selectLanguage(language)
        }

        binding.rvLanguages.adapter = languageAdapter
        languageAdapter.submitList(languages)

        binding.etSearchLanguage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLanguages(s?.toString().orEmpty())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        binding.btnContinue.setOnClickListener {
            MyApplication.applyLanguage(selectedLanguageCode)
            findNavController().navigate(R.id.action_languageFragment_to_onBoardingFragment)
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

        languageAdapter.submitList(filteredLanguages)
        binding.tvEmptyLanguages.isVisible = filteredLanguages.isEmpty()
    }

    private data class LanguageOption(
        val code: String,
        val name: String,
        val nativeName: String
    )

    private class LanguageAdapter(
        private var selectedLanguageCode: String,
        private val onLanguageClick: (LanguageOption) -> Unit
    ) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

        private val languages = mutableListOf<LanguageOption>()

        fun submitList(newLanguages: List<LanguageOption>) {
            languages.clear()
            languages.addAll(newLanguages)
            notifyDataSetChanged()
        }

        fun updateSelection(languageCode: String) {
            val previousIndex = languages.indexOfFirst { it.code == selectedLanguageCode }
            val selectedIndex = languages.indexOfFirst { it.code == languageCode }
            selectedLanguageCode = languageCode

            if (previousIndex >= 0) notifyItemChanged(previousIndex)
            if (selectedIndex >= 0) notifyItemChanged(selectedIndex)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemLanguageBinding.inflate(inflater, parent, false)
            return LanguageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            holder.bind(languages[position], languages[position].code == selectedLanguageCode, onLanguageClick)
        }

        override fun getItemCount(): Int = languages.size

        private class LanguageViewHolder(
            private val binding: ItemLanguageBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(
                language: LanguageOption,
                isSelected: Boolean,
                onLanguageClick: (LanguageOption) -> Unit
            ) {
                binding.tvLanguageName.text = language.name
                binding.tvLanguageNativeName.text = language.nativeName
                binding.languageContainer.setBackgroundResource(
                    if (isSelected) R.drawable.bg_language_item_selected else R.drawable.bg_language_item
                )
                binding.imgLanguageSelected.setImageResource(
                    if (isSelected) R.drawable.ic_language_selected else R.drawable.ic_language_unselected
                )
                binding.root.setOnClickListener { onLanguageClick(language) }
            }
        }
    }
}
