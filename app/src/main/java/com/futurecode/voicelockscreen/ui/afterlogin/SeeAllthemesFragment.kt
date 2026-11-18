package com.futurecode.voicelockscreen.ui.afterlogin

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSeeAllthemesBinding
import com.futurecode.voicelockscreen.databinding.VnItemThemeVerticalBinding
import com.futurecode.voicelockscreen.databinding.VnItemUploadImageBinding
import com.futurecode.voicelockscreen.model.ThemeModel
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore

/** Theme Screen Lock: reference fragment_all_theme — Upload Image header + 2-column theme grid; tap opens Preview/Apply. */
class SeeAllthemesFragment : BaseFragment<FragmentSeeAllthemesBinding>(FragmentSeeAllthemesBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        val grid = GridLayoutManager(requireContext(), 2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (position == 0) 2 else 1
        }
        binding.rvThemes.layoutManager = grid
        binding.rvThemes.adapter = Adapter(themes())
        val gap = resources.getDimensionPixelSize(R.dimen.vn_dp_12)
        binding.rvThemes.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(out: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                val pos = parent.getChildAdapterPosition(v)
                if (pos == RecyclerView.NO_POSITION) return
                out.bottom = gap
                if (pos == 0) return
                val column = (pos - 1) % 2
                out.left = if (column == 0) 0 else gap / 2
                out.right = if (column == 0) gap / 2 else 0
            }
        })
    }

    private fun themes() = listOf(
        ThemeModel(ThemeWallpaperStore.THEME_ONE_ID, R.drawable.theme_one),
        ThemeModel(ThemeWallpaperStore.THEME_TWO_ID, R.drawable.theme_two),
        ThemeModel(ThemeWallpaperStore.THEME_THREE_ID, R.drawable.theme_three),
        ThemeModel(ThemeWallpaperStore.THEME_FOUR_ID, R.drawable.theme_four),
        ThemeModel(ThemeWallpaperStore.THEME_FIVE_ID, R.drawable.theme_five),
        ThemeModel(ThemeWallpaperStore.THEME_SIX_ID, R.drawable.theme_six),
        ThemeModel(ThemeWallpaperStore.THEME_PREVIEW_ID, R.drawable.lock_preview_wallpaper),
    )

    private inner class Adapter(private val items: List<ThemeModel>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        inner class UploadHolder(val b: VnItemUploadImageBinding) : RecyclerView.ViewHolder(b.root)
        inner class ThemeHolder(val b: VnItemThemeVerticalBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemViewType(position: Int) = if (position == 0) 0 else 1
        override fun getItemCount() = items.size + 1
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == 0) UploadHolder(VnItemUploadImageBinding.inflate(inflater, parent, false))
            else ThemeHolder(VnItemThemeVerticalBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is UploadHolder -> holder.b.root.setOnClickListener { findNavController().navigate(R.id.customThemeFragment, null, VoiceNoteUi.slideNavOptions) }
                is ThemeHolder -> {
                    val theme = items[position - 1]
                    Glide.with(holder.b.ivTheme).load(theme.imageResId).centerCrop().into(holder.b.ivTheme)
                    holder.b.root.setOnClickListener {
                        findNavController().navigate(R.id.themePreviewFragment, ThemePreviewFragment.themeArgs(theme.id, theme.imageResId), VoiceNoteUi.slideNavOptions)
                    }
                }
            }
        }
    }
}
