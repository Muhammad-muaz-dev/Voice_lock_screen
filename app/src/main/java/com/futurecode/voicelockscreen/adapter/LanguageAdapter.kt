package com.futurecode.voicelockscreen.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.databinding.ItemLanguageBinding
import com.futurecode.voicelockscreen.model.LanguageOption

class LanguageAdapter(
    private var list: List<LanguageOption>,
    private var selectedLanguageCode: String,
    private val onLanguageClick: (LanguageOption) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLanguageBinding.inflate(inflater, parent, false)
        return LanguageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val item = list[position]
        val isSelected = item.code == selectedLanguageCode
        holder.bind(item, isSelected, onLanguageClick)
    }

    override fun getItemCount(): Int = list.size

    fun submitList(newList: List<LanguageOption>) {
        list = newList
        notifyDataSetChanged()
    }

    fun updateSelection(languageCode: String) {
        val previousIndex = list.indexOfFirst { it.code == selectedLanguageCode }
        val selectedIndex = list.indexOfFirst { it.code == languageCode }
        selectedLanguageCode = languageCode

        if (previousIndex >= 0) notifyItemChanged(previousIndex)
        if (selectedIndex >= 0) notifyItemChanged(selectedIndex)
    }

    class LanguageViewHolder(
        private val binding: ItemLanguageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            language: LanguageOption,
            isSelected: Boolean,
            onLanguageClick: (LanguageOption) -> Unit
        ) {
            binding.tvLanguageName.text = language.name
            binding.tvLanguageNativeName.text = language.nativeName
            binding.tvBadge.text = language.code.uppercase().take(2)
            binding.languageContainer.setBackgroundResource(
                if (isSelected) R.drawable.bg_row_language_selected else R.drawable.bg_row_language
            )
            binding.imgLanguageSelected.setImageResource(
                if (isSelected) R.drawable.bg_radio_selected else R.drawable.bg_radio_unselected
            )
            binding.root.setOnClickListener { onLanguageClick(language) }
        }
    }
}
