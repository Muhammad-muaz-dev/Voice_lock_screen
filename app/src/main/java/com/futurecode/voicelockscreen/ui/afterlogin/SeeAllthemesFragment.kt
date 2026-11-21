package com.futurecode.voicelockscreen.ui.afterlogin

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.api.wallpaper.Wallpaper
import com.futurecode.voicelockscreen.api.wallpaper.WallpaperCategory
import com.futurecode.voicelockscreen.api.wallpaper.WallpaperRepository
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSeeAllthemesBinding
import com.futurecode.voicelockscreen.databinding.ItemWallpaperCategoryBinding
import com.futurecode.voicelockscreen.databinding.VnItemThemeVerticalBinding
import com.futurecode.voicelockscreen.databinding.VnItemUploadImageBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Lock screen themes from the themes API: category chips, then an "Upload image" header and a
 * 2-column wallpaper grid that pages in as you scroll. Tapping a wallpaper opens Preview / Apply.
 */
class SeeAllthemesFragment : BaseFragment<FragmentSeeAllthemesBinding>(FragmentSeeAllthemesBinding::inflate) {

    private var categories: List<WallpaperCategory> = emptyList()
    private var selectedCategoryId: Int? = null
    private var hasNextPage = false
    private var loadJob: Job? = null
    private var pagingInFlight = false

    private val categoryAdapter = CategoryAdapter()
    private val wallpaperAdapter = WallpaperAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Keep the chosen category when returning from Preview (the fragment instance survives).
        savedInstanceState?.getInt(KEY_CATEGORY)?.takeIf { it != 0 }?.let { selectedCategoryId = it }
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnThemesRetry.setOnClickListener { loadCategories(forceRefresh = true) }
        binding.rvCategories.adapter = categoryAdapter
        setupGrid()
        loadCategories(forceRefresh = false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedCategoryId?.let { outState.putInt(KEY_CATEGORY, it) }
    }

    // region Setup

    private fun setupGrid() {
        val grid = GridLayoutManager(requireContext(), GRID_COLUMNS)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (position == 0) GRID_COLUMNS else 1
        }
        binding.rvThemes.layoutManager = grid
        binding.rvThemes.adapter = wallpaperAdapter
        val gap = resources.getDimensionPixelSize(R.dimen.spacing_md)
        binding.rvThemes.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(out: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                val pos = parent.getChildAdapterPosition(v)
                if (pos == RecyclerView.NO_POSITION) return
                out.bottom = gap
                if (pos == 0) return
                val column = (pos - 1) % GRID_COLUMNS
                out.left = if (column == 0) 0 else gap / 2
                out.right = if (column == 0) gap / 2 else 0
            }
        })
        binding.rvThemes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0 || !hasNextPage || pagingInFlight) return
                if (grid.findLastVisibleItemPosition() >= wallpaperAdapter.itemCount - PAGING_THRESHOLD) loadNextPage()
            }
        })
    }

    // endregion

    // region Loading

    private fun loadCategories(forceRefresh: Boolean) {
        loadJob?.cancel()
        showState(loading = true)
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { WallpaperRepository.categories(forceRefresh) }
            val loaded = result.getOrNull()
            if (loaded.isNullOrEmpty()) {
                showState(message = if (result.isFailure) R.string.themes_load_error else R.string.themes_empty, retry = true)
                return@launch
            }
            categories = loaded
            val selected = selectedCategoryId?.takeIf { id -> loaded.any { it.id == id } } ?: loaded.first().id
            selectCategory(selected, forceRefresh)
        }
    }

    private fun selectCategory(categoryId: Int, forceRefresh: Boolean = false) {
        selectedCategoryId = categoryId
        categoryAdapter.notifyDataSetChanged()
        scrollChipIntoView(categoryId)
        loadJob?.cancel()
        showState(loading = true)
        loadJob = viewLifecycleOwner.lifecycleScope.launch {
            val page = runCatching { WallpaperRepository.wallpapers(categoryId, forceRefresh) }.getOrElse {
                showState(message = R.string.themes_load_error, retry = true)
                return@launch
            }
            if (selectedCategoryId != categoryId) return@launch
            hasNextPage = page.hasNext
            wallpaperAdapter.submit(page.items)
            binding.rvThemes.scrollToPosition(0)
            if (page.items.isEmpty()) showState(message = R.string.themes_empty) else showState()
        }
    }

    private fun loadNextPage() {
        val categoryId = selectedCategoryId ?: return
        pagingInFlight = true
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { WallpaperRepository.loadNextPage(categoryId) }.onSuccess { page ->
                if (selectedCategoryId == categoryId) {
                    hasNextPage = page.hasNext
                    wallpaperAdapter.submit(page.items)
                }
            }
            pagingInFlight = false
        }
    }

    /** Loading spinner, message (+ optional retry) or content; the chips stay visible when loaded. */
    private fun showState(loading: Boolean = false, @StringRes message: Int? = null, retry: Boolean = false) {
        val b = bindingOrNull ?: return
        b.progressThemes.isVisible = loading
        b.tvThemesMessage.isVisible = message != null
        message?.let { b.tvThemesMessage.setText(it) }
        b.btnThemesRetry.isVisible = retry
        b.rvThemes.isVisible = !loading && (message == null || !retry)
        b.rvCategories.isVisible = categories.isNotEmpty()
    }

    private fun scrollChipIntoView(categoryId: Int) {
        val index = categories.indexOfFirst { it.id == categoryId }
        if (index >= 0) binding.rvCategories.smoothScrollToPosition(index)
    }

    private fun openPreview(wallpaper: Wallpaper) {
        findNavController().navigate(
            R.id.themePreviewFragment,
            ThemePreviewFragment.wallpaperArgs(wallpaper),
            VoiceNoteUi.slideNavOptions
        )
    }

    // endregion

    // region Adapters

    private inner class CategoryAdapter : RecyclerView.Adapter<CategoryAdapter.Holder>() {
        inner class Holder(val chip: ItemWallpaperCategoryBinding) : RecyclerView.ViewHolder(chip.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemWallpaperCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = categories.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val category = categories[position]
            holder.chip.tvCategory.text = category.name
            holder.chip.tvCategory.isSelected = category.id == selectedCategoryId
            holder.chip.tvCategory.setOnClickListener {
                if (category.id != selectedCategoryId) selectCategory(category.id)
            }
        }
    }

    private inner class WallpaperAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var items: List<Wallpaper> = emptyList()

        inner class UploadHolder(val b: VnItemUploadImageBinding) : RecyclerView.ViewHolder(b.root)
        inner class ThemeHolder(val b: VnItemThemeVerticalBinding) : RecyclerView.ViewHolder(b.root)

        fun submit(newItems: List<Wallpaper>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemViewType(position: Int) = if (position == 0) TYPE_UPLOAD else TYPE_THEME
        override fun getItemCount() = items.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_UPLOAD) UploadHolder(VnItemUploadImageBinding.inflate(inflater, parent, false))
            else ThemeHolder(VnItemThemeVerticalBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is UploadHolder -> holder.b.root.setOnClickListener {
                    findNavController().navigate(R.id.customThemeFragment, null, VoiceNoteUi.slideNavOptions)
                }
                is ThemeHolder -> {
                    val wallpaper = items[position - 1]
                    Glide.with(holder.b.ivTheme)
                        .load(wallpaper.fileUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        // Decode at cell size: originals are up to ~2 MB.
                        .override(THUMB_WIDTH_PX, THUMB_HEIGHT_PX)
                        .placeholder(R.color.vn_neutral09)
                        .transition(com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade())
                        .centerCrop()
                        .into(holder.b.ivTheme)
                    holder.b.ivTheme.contentDescription = wallpaper.title ?: getString(R.string.preview)
                    holder.b.ivPremium.isVisible = wallpaper.isPremium && !prefManager.isPro
                    holder.b.root.setOnClickListener { openPreview(wallpaper) }
                }
            }
        }
    }

    // endregion

    private companion object {
        const val KEY_CATEGORY = "wallpaper_category"
        const val GRID_COLUMNS = 2
        const val PAGING_THRESHOLD = 6
        const val TYPE_UPLOAD = 0
        const val TYPE_THEME = 1
        const val THUMB_WIDTH_PX = 360
        const val THUMB_HEIGHT_PX = 640
    }
}
