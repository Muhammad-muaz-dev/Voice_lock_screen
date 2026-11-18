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
    private data class Row(val language: LanguageOption, val child: Boolean = false)
    private val expanded = mutableSetOf<String>()
    private var rows = buildRows()

    private fun buildRows(): List<Row> = buildList {
        languages.forEach { language ->
            add(Row(language))
            if (language.code in expanded) language.variants.forEach { add(Row(it, true)) }
        }
    }

    fun restoreExpanded(codes: Collection<String>) {
        expanded.clear()
        expanded.addAll(codes)
        rows = buildRows()
        notifyDataSetChanged()
    }

    fun expandedCodes(): ArrayList<String> = ArrayList(expanded)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = LanguageViewHolder(
        ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val row = rows[position]
        holder.bind(row.language, row.child)
    }

    override fun getItemCount() = rows.size

    fun updateSelection(languageCode: String) {
        selectedLanguageCode = languageCode
        notifyDataSetChanged()
    }

    inner class LanguageViewHolder(private val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(language: LanguageOption, child: Boolean) {
            val group = language.variants.isNotEmpty()
            val isSelected = !group && language.code == selectedLanguageCode
            val isExpanded = language.code in expanded
            val params = binding.root.layoutParams as ViewGroup.MarginLayoutParams
            params.marginStart = if (child) binding.root.resources.getDimensionPixelSize(R.dimen.spacing_xxxl) else 0
            binding.root.layoutParams = params
            binding.tvLanguageName.text = language.nativeName
            binding.imgFlag.country = language.country
            binding.imgFlag.englishVariant = child && language.code.startsWith("en-")
            binding.languageContainer.setBackgroundResource(
                if (isSelected) R.drawable.bg_language_picker_row_selected else R.drawable.bg_language_picker_row
            )
            binding.imgLanguageSelected.setImageResource(
                if (group) R.drawable.ic_language_expand
                else if (isSelected) R.drawable.ic_language_selected else R.drawable.ic_language_unselected
            )
            binding.imgLanguageSelected.rotation = if (group && isExpanded) 180f else 0f
            binding.root.isSelected = isSelected
            ViewCompat.setStateDescription(binding.root, binding.root.context.getString(
                if (group) { if (isExpanded) R.string.language_expanded else R.string.language_collapsed }
                else { if (isSelected) R.string.language_selected else R.string.language_not_selected }
            ))
            binding.root.setOnClickListener {
                if (group) {
                    if (!expanded.add(language.code)) expanded.remove(language.code)
                    rows = buildRows()
                    notifyDataSetChanged()
                } else onLanguageClick(language)
            }
        }
    }
}
