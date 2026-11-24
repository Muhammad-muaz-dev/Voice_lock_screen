package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.ads.AdPlacement
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentThemesBinding
import com.futurecode.voicelockscreen.utils.ThemePreviewBinder
import com.futurecode.voicelockscreen.utils.bindBottomTabs

class ThemesFragment : BaseFragment<FragmentThemesBinding>(FragmentThemesBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindBottomTabs(binding.bottomNav, 1)
        ThemePreviewBinder.applySampleQuoteGradient(binding.tvSampleQuote)
        binding.ivSettings.setOnClickListener { findNavController().navigate(R.id.settingFragment) }
        binding.ivPremium.setOnClickListener { findNavController().navigate(R.id.premiumFragment) }
        val openQuote = View.OnClickListener { findNavController().navigate(R.id.aiQuoteFragment, null, VoiceNoteUi.slideNavOptions) }
        binding.cardAiQuote.setOnClickListener(openQuote)
        binding.btnAiQuoteNext.setOnClickListener(openQuote)
        val openThemes = View.OnClickListener { findNavController().navigate(R.id.seeAllthemesFragment, null, VoiceNoteUi.slideNavOptions) }
        binding.cardLockScreenTheme.setOnClickListener(openThemes)
        binding.btnLockThemeNext.setOnClickListener(openThemes)
        AdPlacement.loadAndShowNative(viewLifecycleOwner, binding.themesNativeAdContainer)
    }
}
