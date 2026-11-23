package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.activity.addCallback
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.ui.prelogin.PreloginFlow
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.BuildConfig
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPremiumBinding
import com.futurecode.voicelockscreen.utils.DarkDialogs

class PremiumFragment : BaseFragment<FragmentPremiumBinding>(FragmentPremiumBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivClose.setOnClickListener { close() }
        if (inLaunchFlow) {
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { close() }
        }
        binding.tvRestore.setOnClickListener {
            if (BuildConfig.DEBUG && requireContext().getSharedPreferences("local_pro",0).getBoolean("purchased",false)) {
                prefManager.isPro = true
                showSuccess()
            } else showFail()
        }
        binding.btnPurchase.setOnClickListener { completePurchase() }
        if (prefManager.isPro) showSuccess()
    }

    private val inLaunchFlow: Boolean
        get() = arguments?.getBoolean(PreloginFlow.ARG_IN_FLOW) == true

    /** In the launch flow, leaving Premium moves on to the next step; elsewhere it just pops. */
    private fun close() {
        if (inLaunchFlow) PreloginFlow.continueFrom(this, PreloginFlow.Step.INAPP)
        else findNavController().popBackStack()
    }

    private fun completePurchase() {
        if (!BuildConfig.DEBUG) { showFail(); return }
        requireContext().getSharedPreferences("local_pro",0).edit().putBoolean("purchased",true).apply()
        prefManager.isPro = true
        prefManager.adsOff = true
        showSuccess()
    }

    private fun showSuccess() {
        binding.successOverlay.root.isVisible = true
        binding.successOverlay.successScreen.bindSuccess(R.string.youre_in, R.string.thanks_pro, R.string.lets_go, ::close)
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
