package com.futurecode.voicelockscreen.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
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
    private val onWallpaperClick: (Wallpaper) -> Unit
) : ListAdapter<Wallpaper?, HomeWallpaperAdapter.WallpaperHolder>(WallpaperDiff) {

    class WallpaperHolder(val binding: ItemHomeWallpaperBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WallpaperHolder =
        WallpaperHolder(
            ItemHomeWallpaperBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: WallpaperHolder, position: Int) {
        val wallpaper = getItem(position)
        val binding = holder.binding
        val image = binding.ivWallpaper
        val requests = Glide.with(image)
        requests.clear(image)
        image.setImageDrawable(null)
        binding.wallpaperShimmer.isVisible = true
        binding.root.isFocusable = wallpaper != null
        binding.root.contentDescription = null
        binding.root.setOnClickListener(null)
        binding.root.isClickable = wallpaper != null
        if (wallpaper == null) return

        requests.load(wallpaper.fileUrl)
            .error(R.color.surface)
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?, model: Any?, target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.wallpaperShimmer.isVisible = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable, model: Any, target: Target<Drawable>?,
                    dataSource: DataSource, isFirstResource: Boolean
                ): Boolean {
                    binding.wallpaperShimmer.isVisible = false
                    return false
                }
            })
            .into(image)

        binding.root.contentDescription = wallpaper.title?.takeIf { it.isNotBlank() }
            ?: image.context.getString(R.string.preview)
        binding.root.setOnClickListener { onWallpaperClick(wallpaper) }
    }
    private object WallpaperDiff : DiffUtil.ItemCallback<Wallpaper>() {
        override fun areItemsTheSame(oldItem: Wallpaper, newItem: Wallpaper) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Wallpaper, newItem: Wallpaper) =
            oldItem == newItem
    }
}
