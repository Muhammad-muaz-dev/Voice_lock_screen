package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSettingBinding

class SettingFragment : BaseFragment<FragmentSettingBinding>(FragmentSettingBinding::inflate) {



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnUpgrade.setOnClickListener {
            // Handle Premium Upgrade
        }

        binding.clChangeLanguage.setOnClickListener {
            // Handle Language Change
        }

        binding.clSecurityQuestion.setOnClickListener {
            // Handle Security Question Change
        }

        binding.clDisableLock.setOnClickListener {
            // Handle Disable System Lock
        }

        binding.clShareApp.setOnClickListener {
            // Handle Share App
        }

        binding.clFeedback.setOnClickListener {
            // Handle Feedback
        }

        binding.clPrivacy.setOnClickListener {
            // Handle Privacy Policy
        }
    }

}