package com.futurecode.voicelockscreen.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ItemLanguageBinding
import com.futurecode.voicelockscreen.model.LanguageOption

class LanguageAdapter(
    private val languages: List<LanguageOption>,
    private var selectedLanguageCode: String,
    private val onLanguageClick: (LanguageOption) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LanguageViewHolder(
        ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    override fun getItemCount() = languages.size

    fun updateSelection(languageCode: String) {
        selectedLanguageCode = languageCode
        notifyDataSetChanged()
    }

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(language: LanguageOption) {
            val isSelected = language.code == selectedLanguageCode
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            params.marginStart = 0
            binding.root.layoutParams = params

            binding.tvLanguageName.text = language.name
            binding.languageContainer.setBackgroundResource(
                if (isSelected) R.drawable.bg_language_picker_row_selected
                else R.drawable.bg_language_picker_row
            )
            binding.languageFlag.country = language.country
            binding.root.isSelected = isSelected
            ViewCompat.setStateDescription(
                binding.root,
                binding.root.context.getString(
                    if (isSelected) R.string.language_selected else R.string.language_not_selected
                )
            )
            binding.root.setOnClickListener { onLanguageClick(language) }
        }
    }
}
