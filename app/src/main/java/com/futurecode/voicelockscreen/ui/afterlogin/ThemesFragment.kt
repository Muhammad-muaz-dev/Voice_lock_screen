package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentThemesBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.bindBottomTabs

class ThemesFragment : BaseFragment<FragmentThemesBinding>(FragmentThemesBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindBottomTabs(binding.bottomNav, 1)
        binding.tvQuotePreview.text = VoiceLockFeatureStore.lockQuote(requireContext())
        binding.ivSettings.setOnClickListener { findNavController().navigate(R.id.settingFragment) }
        binding.ivPremium.setOnClickListener { findNavController().navigate(R.id.premiumFragment) }
        binding.btnMakeQuote.setOnClickListener { findNavController().navigate(R.id.aiQuoteFragment) }
        binding.cardQuote.setOnClickListener { findNavController().navigate(R.id.aiQuoteFragment) }
        binding.btnPickTheme.setOnClickListener { findNavController().navigate(R.id.seeAllthemesFragment) }
        binding.cardTheme.setOnClickListener { findNavController().navigate(R.id.seeAllthemesFragment) }
    }
}
