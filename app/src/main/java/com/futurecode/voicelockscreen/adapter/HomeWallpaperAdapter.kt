package com.futurecode.voicelockscreen.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.api.wallpaper.Wallpaper
import com.futurecode.voicelockscreen.databinding.ItemHomeWallpaperBinding

class HomeWallpaperAdapter(
    private val onWallpaperClick: (Wallpaper) -> Unit,
    private val onRetryThemes: () -> Unit
) : ListAdapter<HomeWallpaperAdapter.Tile, HomeWallpaperAdapter.WallpaperHolder>(TileDiff) {

    sealed interface Tile {
        data object Loading : Tile
        data object Retry : Tile
        data class Preview(val wallpaper: Wallpaper) : Tile
    }

    class WallpaperHolder(val binding: ItemHomeWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperHolder =
        WallpaperHolder(
            ItemHomeWallpaperBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: WallpaperHolder, position: Int) {
        val binding = holder.binding
        Glide.with(binding.ivWallpaper).clear(binding.ivWallpaper)
        when (val tile = getItem(position)) {
            Tile.Loading -> showLoading(binding)
            Tile.Retry -> showRetry(binding, onRetryThemes)
            is Tile.Preview -> loadPreview(binding, tile.wallpaper)
        }
    }

    override fun onViewRecycled(holder: WallpaperHolder) {
        Glide.with(holder.binding.ivWallpaper).clear(holder.binding.ivWallpaper)
        super.onViewRecycled(holder)
    }

    private fun loadPreview(binding: ItemHomeWallpaperBinding, wallpaper: Wallpaper) {
        showLoading(binding)
        Glide.with(binding.ivWallpaper)
            .load(wallpaper.fileUrl)
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    showRetry(binding) { loadPreview(binding, wallpaper) }
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>?,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    binding.wallpaperShimmer.isVisible = false
                    val label = wallpaper.title?.takeIf { it.isNotBlank() }
                        ?: binding.root.context.getString(R.string.preview)
                    bindAction(binding, label) { onWallpaperClick(wallpaper) }
                    return false
                }
            })
            .into(binding.ivWallpaper)
    }

    private fun showLoading(binding: ItemHomeWallpaperBinding) {
        binding.ivWallpaper.isVisible = true
        binding.wallpaperShimmer.isVisible = true
        binding.ivWallpaperRetry.isVisible = false
        binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.surface))
        bindAction(binding)
    }

    private fun showRetry(binding: ItemHomeWallpaperBinding, onRetry: () -> Unit) {
        binding.ivWallpaper.isVisible = false
        binding.wallpaperShimmer.isVisible = false
        binding.ivWallpaperRetry.isVisible = true
        binding.root.setCardBackgroundColor(ContextCompat.getColor(binding.root.context, R.color.surfaceSunken))
        bindAction(binding, binding.root.context.getString(R.string.themes_retry), onRetry)
    }

    private fun bindAction(
        binding: ItemHomeWallpaperBinding,
        label: String? = null,
        action: (() -> Unit)? = null
    ) {
        binding.root.contentDescription = label
        binding.root.setOnClickListener(action?.let { click -> View.OnClickListener { click() } })
        binding.root.isClickable = action != null
        binding.root.isFocusable = action != null
    }

    private object TileDiff : DiffUtil.ItemCallback<Tile>() {
        override fun areItemsTheSame(oldItem: Tile, newItem: Tile): Boolean =
            if (oldItem is Tile.Preview && newItem is Tile.Preview) {
                oldItem.wallpaper.id == newItem.wallpaper.id
            } else {
                oldItem == newItem
            }

        override fun areContentsTheSame(oldItem: Tile, newItem: Tile) = oldItem == newItem
    }
}
