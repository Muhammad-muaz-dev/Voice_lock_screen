package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.BuildConfig
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPremiumBinding
import com.futurecode.voicelockscreen.utils.DarkDialogs

class PremiumFragment : BaseFragment<FragmentPremiumBinding>(FragmentPremiumBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivClose.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.tvRestore.setOnClickListener {
            if (BuildConfig.DEBUG && requireContext().getSharedPreferences("local_pro",0).getBoolean("purchased",false)) {
                prefManager.isPro = true
                showSuccess()
            } else showFail()
        }
        binding.btnPurchase.setOnClickListener { completePurchase() }
        binding.btnLetsGo.setOnClickListener { findNavController().popBackStack() }
        if (prefManager.isPro) showSuccess()
    }

    private fun completePurchase() {
        if (!BuildConfig.DEBUG) { showFail(); return }
        requireContext().getSharedPreferences("local_pro",0).edit().putBoolean("purchased",true).apply()
        prefManager.isPro = true
        prefManager.adsOff = true
        showSuccess()
    }

    private fun showSuccess() {
        binding.successOverlay.isVisible = true
    }

    private fun showFail() {
        DarkDialogs.show(
            requireContext(),
            getString(R.string.purchase_unavailable),
            getString(R.string.purchase_unavailable_body),
            getString(R.string.close),
            getString(R.string.try_again),
            onPositive = { completePurchase() }
        )
    }

}
