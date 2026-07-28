package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentFingerprintLockBinding

class FingerprintLockFragment : BaseFragment<FragmentFingerprintLockBinding>(FragmentFingerprintLockBinding::inflate) {


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEnable.setOnClickListener {
            // Logic to enable fingerprint lock
        }
    }


}