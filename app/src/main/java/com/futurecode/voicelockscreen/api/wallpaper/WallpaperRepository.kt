package com.futurecode.voicelockscreen.api.wallpaper

import android.content.Context
import android.net.Uri
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Categories and wallpapers from [WallpaperApi], cached in memory for the app session.
 * Wallpapers are paged; [loadNextPage] appends the next page for a category.
 */
object WallpaperRepository {

    data class CategoryPage(val items: List<Wallpaper>, val hasNext: Boolean, val nextPage: Int)

    private var categoriesCache: List<WallpaperCategory>? = null
    private val pages = mutableMapOf<Int, CategoryPage>()

    suspend fun categories(forceRefresh: Boolean = false): List<WallpaperCategory> {
        if (!forceRefresh) categoriesCache?.let { return it }
        val all = mutableListOf<WallpaperCategory>()
        var page = 1
        do {
            val response = withContext(Dispatchers.IO) { WallpaperApi.instance.categories(page) }
            all += response.data.orEmpty()
            page++
        } while (response.meta?.hasNext == true)
        // Hide empty categories; keep the server order.
        return all.filter { it.artworkCount > 0 }.also { categoriesCache = it }
    }

    /** Cached wallpapers for [categoryId], or the first page when nothing is cached yet. */
    suspend fun wallpapers(categoryId: Int, forceRefresh: Boolean = false): CategoryPage {
        if (!forceRefresh) pages[categoryId]?.let { return it }
        return fetch(categoryId, page = 1, existing = emptyList())
    }

    suspend fun loadNextPage(categoryId: Int): CategoryPage {
        val current = pages[categoryId] ?: return wallpapers(categoryId)
        if (!current.hasNext) return current
        return fetch(categoryId, current.nextPage, current.items)
    }

    private suspend fun fetch(categoryId: Int, page: Int, existing: List<Wallpaper>): CategoryPage {
        val response = withContext(Dispatchers.IO) { WallpaperApi.instance.wallpapers(categoryId, page) }
        val images = response.data.orEmpty().filter { it.mediaType == null || it.mediaType == "image" }
        val merged = (existing + images).distinctBy { it.id }
        return CategoryPage(merged, response.meta?.hasNext == true, page + 1).also { pages[categoryId] = it }
    }

    /**
     * Downloads [wallpaper] into app-private storage so the lock screen can show it offline
     * and instantly. Returns a file Uri for [com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore.saveCustomThemeUri].
     */
    suspend fun downloadForLockScreen(context: Context, wallpaper: Wallpaper): Uri = withContext(Dispatchers.IO) {
        val cached = Glide.with(context.applicationContext).downloadOnly().load(wallpaper.fileUrl).submit().get()
        val dir = File(context.filesDir, WALLPAPER_DIR).apply { mkdirs() }
        // One applied wallpaper at a time; a new file name forces image views to reload.
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, "wallpaper_${wallpaper.id}_${System.currentTimeMillis()}.img")
        cached.copyTo(target, overwrite = true)
        Uri.fromFile(target)
    }

    private const val WALLPAPER_DIR = "lock_wallpapers"
}
