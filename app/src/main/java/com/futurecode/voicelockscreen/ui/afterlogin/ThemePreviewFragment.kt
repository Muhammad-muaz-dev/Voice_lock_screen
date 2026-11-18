package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentThemePreviewBinding
import com.futurecode.voicelockscreen.utils.ThemePreviewBinder
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
        ThemePreviewBinder.bindContent(binding, requireContext(), quote)
        if (themeId != NO_THEME && themeRes != 0) {
            binding.ivTheme.setImageResource(themeRes)
            binding.btnApply.isVisible = true
            binding.btnApply.setOnClickListener {
                ThemeWallpaperStore.saveSelectedTheme(requireContext(), themeId, themeRes)
                Toast.makeText(requireContext(), R.string.theme_applied, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        } else {
            ThemeWallpaperStore.applyTo(binding.ivTheme, requireContext())
            binding.btnApply.isVisible = false
        }
    }

    companion object {
        private const val ARG_THEME_ID = "theme_id"
        private const val ARG_THEME_RES = "theme_res"
        private const val ARG_QUOTE = "quote"
        private const val NO_THEME = -1
        fun themeArgs(themeId: Int, themeRes: Int) = bundleOf(ARG_THEME_ID to themeId, ARG_THEME_RES to themeRes)
        fun quoteArgs(quote: String) = bundleOf(ARG_QUOTE to quote)
    }
}
