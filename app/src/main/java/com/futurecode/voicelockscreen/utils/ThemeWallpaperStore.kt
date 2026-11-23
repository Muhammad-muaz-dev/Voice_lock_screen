package com.futurecode.voicelockscreen.utils

import android.content.Context
import androidx.annotation.DrawableRes
import com.futurecode.voicelockscreen.R

object ThemeWallpaperStore {
    const val THEME_DEFAULT_ID = 0
    const val THEME_ONE_ID = 1
    const val THEME_TWO_ID = 2
    const val THEME_THREE_ID = 3
    const val THEME_FOUR_ID = 4
    const val THEME_FIVE_ID = 5
    const val THEME_SIX_ID = 6
    const val THEME_PREVIEW_ID = 7

    private const val PREFS = "theme_wallpaper_prefs"
    private const val KEY_THEME_ID = "selected_theme_id"
    private const val KEY_WALLPAPER_NAME = "selected_wallpaper_name"

    fun saveSelectedTheme(context: Context, themeId: Int, @DrawableRes wallpaperRes: Int) {
        VoiceLockFeatureStore.saveCustomThemeUri(context, null)
        VoiceLockFeatureStore.setShowClockAndQuote(context, true)
        val wallpaperName = context.resources.getResourceEntryName(wallpaperRes)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_ID, themeId)
            .putString(KEY_WALLPAPER_NAME, wallpaperName)
            .apply()
    }

    fun applyTo(image: android.widget.ImageView, context: Context) {
        val uri = VoiceLockFeatureStore.customThemeUri(context)
        if (uri != null && uri.scheme == "file") {
            val file = java.io.File(uri.path.orEmpty())
            if (file.exists()) {
                com.bumptech.glide.Glide.with(context.applicationContext)
                    .load(file)
                    .centerCrop()
                    .error(getSelectedWallpaperRes(context))
                    .into(image)
                return
            }
        }
        if (uri != null) {
            val loaded = runCatching { image.setImageURI(uri); image.drawable != null }.getOrDefault(false)
            if (loaded) return
        }
        image.setImageResource(getSelectedWallpaperRes(context))
    }

    fun getSelectedThemeId(context: Context): Int {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_THEME_ID, THEME_DEFAULT_ID)
    }

    @DrawableRes
    fun getSelectedWallpaperRes(context: Context): Int {
        val wallpaperName = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_WALLPAPER_NAME, null)
            ?: return R.drawable.lock_preview_wallpaper

        val resolvedId = context.resources.getIdentifier(
            wallpaperName,
            "drawable",
            context.packageName
        )
        return if (resolvedId != 0) resolvedId else R.drawable.lock_preview_wallpaper
    }
}
