package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPermissionBinding

class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnContinue.setOnClickListener {
            // Logic to move to next screen if permissions are granted
        }

        // Initialize permission status
        updatePermissionStatus()

        binding.swPhone.setOnCheckedChangeListener { _, _ ->
            updatePermissionStatus()
        }

        binding.swOverlay.setOnCheckedChangeListener { _, _ ->
            updatePermissionStatus()
        }
    }

    private fun updatePermissionStatus() {
        var count = 0
        if (binding.swPhone.isChecked) count++
        if (binding.swOverlay.isChecked) count++

        binding.tvEnabledCount.text = getString(com.futurecode.voicelockscreen.R.string.permissions_enabled_count, count, 2)
        
        // Update button state based on permissions
        if (count == 2) {
            binding.btnContinue.backgroundTintList = null // Use original gradient
            binding.btnContinue.setTextColor(resources.getColor(android.R.color.white, null))
            binding.btnContinue.isEnabled = true
        } else {
            binding.btnContinue.isEnabled = false
        }
    }

}