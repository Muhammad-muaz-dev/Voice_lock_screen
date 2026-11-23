package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.graphics.Insets
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.futurecode.voicelockscreen.api.wallpaper.Wallpaper
import com.futurecode.voicelockscreen.api.wallpaper.WallpaperRepository
import kotlinx.coroutines.launch
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentThemePreviewBinding
import com.futurecode.voicelockscreen.utils.LockScreenUi
import com.futurecode.voicelockscreen.utils.ThemeWallpaperStore
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore

/**
 * Reference fragment_lock_screen_preview.
 * Theme mode: shows the picked theme with Apply. Quote mode: shows the new quote on the current wallpaper.
 */
class ThemePreviewFragment : BaseFragment<FragmentThemePreviewBinding>(FragmentThemePreviewBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val themeId = args.getInt(ARG_THEME_ID, NO_THEME)
        val themeRes = args.getInt(ARG_THEME_RES, 0)
        val quote = args.getString(ARG_QUOTE) ?: VoiceLockFeatureStore.lockQuote(requireContext())
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        LockScreenUi.bind(binding.lockScreen, quote = quote, showClockAndQuote = true)
        setupEdgeToEdgePreview()
        val wallpaperUrl = args.getString(ARG_WALLPAPER_URL)
        if (wallpaperUrl != null) {
            bindRemoteWallpaper(
                Wallpaper(
                    id = args.getInt(ARG_WALLPAPER_ID),
                    categoryId = 0,
                    title = null,
                    mediaType = "image",
                    isPremium = args.getBoolean(ARG_WALLPAPER_PREMIUM),
                    fileUrl = wallpaperUrl
                )
            )
        } else if (themeId != NO_THEME && themeRes != 0) {
            binding.lockScreen.imgWallpaper.setImageResource(themeRes)
            binding.btnApply.isVisible = true
            binding.btnApply.setOnClickListener {
                ThemeWallpaperStore.saveSelectedTheme(requireContext(), themeId, themeRes)
                Toast.makeText(requireContext(), R.string.theme_applied, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            ThemeWallpaperStore.applyTo(binding.lockScreen.imgWallpaper, requireContext())
            binding.btnApply.isVisible = false
        }
    }

    private fun setupEdgeToEdgePreview() {
        val views = binding
        val content = views.lockScreen.lockContent
        val scroll = content.parent as ViewGroup
        val scene = views.lockScreen.root
        val viewport = ThemePreviewViewport(requireContext())
        val constraints = scroll.layoutParams as ConstraintLayout.LayoutParams
        scroll.removeView(content)
        scene.removeView(scroll)
        viewport.addView(content)
        scene.addView(viewport, constraints)

        val spacing = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        val applyMargin = resources.getDimensionPixelSize(R.dimen.spacing_lg)
        var safe = Insets.NONE
        fun updateViewport() {
            val toolbarHeight = maxOf(views.ivBack.height, views.tvTitle.height)
            val applySpace = if (views.btnApply.isVisible) views.btnApply.height + 2 * spacing else 0
            viewport.updatePadding(
                left = safe.left,
                top = safe.top + toolbarHeight,
                right = safe.right,
                bottom = safe.bottom + applySpace
            )
        }

        listOf(views.ivBack, views.tvTitle, views.btnApply).forEach { overlay ->
            overlay.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateViewport() }
        }
        ViewCompat.setOnApplyWindowInsetsListener(views.root) { _, insets ->
            safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val rtl = views.root.layoutDirection == View.LAYOUT_DIRECTION_RTL
            views.ivBack.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = safe.top
                marginStart = if (rtl) safe.right else safe.left
            }
            views.tvTitle.updateLayoutParams<ConstraintLayout.LayoutParams> {
                marginEnd = spacing + if (rtl) safe.left else safe.right
            }
            views.btnApply.updateLayoutParams<ConstraintLayout.LayoutParams> {
                marginStart = applyMargin + if (rtl) safe.right else safe.left
                marginEnd = applyMargin + if (rtl) safe.left else safe.right
                bottomMargin = spacing + safe.bottom
            }
            updateViewport()
            insets
        }
        ViewCompat.requestApplyInsets(views.root)
    }

    /** API wallpaper: preview from the URL; Apply downloads it once and uses it on the lock screen. */
    private fun bindRemoteWallpaper(wallpaper: Wallpaper) {
        Glide.with(binding.lockScreen.imgWallpaper).load(wallpaper.fileUrl).centerCrop().into(binding.lockScreen.imgWallpaper)
        binding.btnApply.isVisible = true
        binding.btnApply.setOnClickListener {
            if (wallpaper.isPremium && !prefManager.isPro) {
                findNavController().navigate(R.id.premiumFragment)
                return@setOnClickListener
            }
            applyRemoteWallpaper(wallpaper)
        }
    }

    private fun applyRemoteWallpaper(wallpaper: Wallpaper) {
        val button = binding.btnApply
        val originalLabel = (button as? android.widget.TextView)?.text
        button.isEnabled = false
        (button as? android.widget.TextView)?.setText(R.string.themes_applying)
        viewLifecycleOwner.lifecycleScope.launch {
            val context = requireContext().applicationContext
            val result = runCatching { WallpaperRepository.downloadForLockScreen(context, wallpaper) }
            val b = bindingOrNull ?: return@launch
            b.btnApply.isEnabled = true
            (b.btnApply as? android.widget.TextView)?.text = originalLabel
            result.onSuccess { uri ->
                VoiceLockFeatureStore.saveCustomThemeUri(context, uri)
                VoiceLockFeatureStore.setShowClockAndQuote(context, true)
                Toast.makeText(context, R.string.theme_applied, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }.onFailure {
                Toast.makeText(context, R.string.themes_apply_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val ARG_WALLPAPER_ID = "wallpaper_id"
        private const val ARG_WALLPAPER_URL = "wallpaper_url"
        private const val ARG_WALLPAPER_PREMIUM = "wallpaper_premium"
        fun wallpaperArgs(wallpaper: Wallpaper) = bundleOf(
            ARG_WALLPAPER_ID to wallpaper.id,
            ARG_WALLPAPER_URL to wallpaper.fileUrl,
            ARG_WALLPAPER_PREMIUM to wallpaper.isPremium
        )
        private const val ARG_THEME_ID = "theme_id"
        private const val ARG_THEME_RES = "theme_res"
        private const val ARG_QUOTE = "quote"
        private const val NO_THEME = -1
        fun themeArgs(themeId: Int, themeRes: Int) = bundleOf(ARG_THEME_ID to themeId, ARG_THEME_RES to themeRes)
        fun quoteArgs(quote: String) = bundleOf(ARG_QUOTE to quote)
    }
}
