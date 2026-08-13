package com.futurecode.voicelockscreen.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.waterfall.AdWaterfallNativeAdsHelper
import com.futurecode.voicelockscreen.databinding.ItemLanguageBinding
import com.futurecode.voicelockscreen.databinding.ItemNativeAdsAdapterBinding
import com.futurecode.voicelockscreen.model.LanguageOption

object AdViewTypeManager {
    const val TYPE_ITEM = 0
    const val TYPE_AD = 1
}

class LanguageAdapter(
    private val activity: Activity,
    private var list: List<Any>,
    private var selectedLanguageCode: String,
    private val onLanguageClick: (LanguageOption) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val nativeAdsHelper = AdWaterfallNativeAdsHelper(activity)

    override fun getItemViewType(position: Int): Int {
        val item = list[position]
        return if (item is String && item == "AD_UNIT") {
            AdViewTypeManager.TYPE_AD
        } else {
            AdViewTypeManager.TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == AdViewTypeManager.TYPE_AD) {
            val adBinding = ItemNativeAdsAdapterBinding.inflate(inflater, parent, false)
            AdViewHolder(adBinding)
        } else {
            val binding = ItemLanguageBinding.inflate(inflater, parent, false)
            LanguageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        if (holder is LanguageViewHolder && item is LanguageOption) {
            val isSelected = item.code == selectedLanguageCode
            holder.bind(item, isSelected, onLanguageClick)
        } else if (holder is AdViewHolder) {
            holder.bindAd()
        }
    }

    override fun getItemCount(): Int = list.size

    fun submitList(newList: List<Any>) {
        list = newList
        notifyDataSetChanged()
    }

    fun updateSelection(languageCode: String) {
        val previousIndex = list.indexOfFirst { it is LanguageOption && it.code == selectedLanguageCode }
        val selectedIndex = list.indexOfFirst { it is LanguageOption && it.code == languageCode }
        selectedLanguageCode = languageCode

        if (previousIndex >= 0) notifyItemChanged(previousIndex)
        if (selectedIndex >= 0) notifyItemChanged(selectedIndex)
    }

    fun release() {
        nativeAdsHelper.destroy()
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
            binding.languageContainer.setBackgroundResource(
                if (isSelected) R.drawable.bg_language_item_selected else R.drawable.bg_language_item
            )
            binding.imgLanguageSelected.setImageResource(
                if (isSelected) R.drawable.ic_language_selected else R.drawable.ic_language_unselected
            )
            binding.root.setOnClickListener { onLanguageClick(language) }
        }
    }

    inner class AdViewHolder(
        private val binding: ItemNativeAdsAdapterBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindAd() {
            nativeAdsHelper.showNativeAd(
                binding.frameLayout,
                binding.relativeLayout,
                binding.placeholder
            )
        }
    }
}
