package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPremiumBinding

class PremiumFragment : BaseFragment<FragmentPremiumBinding>(FragmentPremiumBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.clWeekly.setOnClickListener {
            updatePlanSelection(it)
        }

        binding.clMonthly.setOnClickListener {
            updatePlanSelection(it)
        }

        binding.clYearly.setOnClickListener {
            updatePlanSelection(it)
        }

        binding.btnPurchase.setOnClickListener {
            // Handle billing logic
        }
    }

    private fun updatePlanSelection(selectedView: View) {
        // Reset all backgrounds
        binding.clWeekly.setBackgroundResource(R.drawable.bg_plan_unselected)
        binding.clMonthly.setBackgroundResource(R.drawable.bg_plan_unselected)
        binding.clYearly.setBackgroundResource(R.drawable.bg_plan_unselected)

        // Set selected background
        selectedView.setBackgroundResource(R.drawable.bg_plan_selected)
        
        // Handle "Best Value" tag visibility or other visual cues if needed
        // In the design, Yearly is the one with the "Best Value" tag.
    }
}
