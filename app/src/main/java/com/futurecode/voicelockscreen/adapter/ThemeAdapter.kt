package com.futurecode.voicelockscreen.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.databinding.ItemThemeBinding
import com.futurecode.voicelockscreen.model.ThemeModel

class ThemeAdapter(
    private var themeList: List<ThemeModel>,
    private var selectedThemeId: Int,
    private val onItemClick: (ThemeModel) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    inner class ThemeViewHolder(val binding: ItemThemeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val binding = ItemThemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ThemeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val theme = themeList[position]

        holder.binding.ivTheme.setImageResource(theme.imageResId)
        holder.binding.ivSelected.visibility =
            if (theme.id == selectedThemeId) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            val currentPosition = holder.bindingAdapterPosition
            if (currentPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            val previousSelectedPosition = themeList.indexOfFirst { it.id == selectedThemeId }
            selectedThemeId = theme.id
            if (previousSelectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelectedPosition)
            }
            notifyItemChanged(currentPosition)
            onItemClick(theme)
        }
    }

    override fun getItemCount(): Int = themeList.size

    fun updateData(newList: List<ThemeModel>) {
        themeList = newList
        notifyDataSetChanged()
    }

    fun setSelectedTheme(themeId: Int) {
        val previousSelectedPosition = themeList.indexOfFirst { it.id == selectedThemeId }
        val newSelectedPosition = themeList.indexOfFirst { it.id == themeId }
        selectedThemeId = themeId
        if (previousSelectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelectedPosition)
        }
        if (newSelectedPosition != RecyclerView.NO_POSITION) {
            notifyItemChanged(newSelectedPosition)
        }
    }
}
