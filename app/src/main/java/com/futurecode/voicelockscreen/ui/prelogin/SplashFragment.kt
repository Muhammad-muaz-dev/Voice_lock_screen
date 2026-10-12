package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSplashBinding
import com.futurecode.voicelockscreen.utils.JsonReadUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        JsonReadUtils.fetchJsonData(requireContext()) { }
        navigation()
    }

     fun navigation(){

         viewLifecycleOwner.lifecycleScope.launch {
             delay(1200)
             if (prefManager.isOnboardingDone) {
                 startActivity(Intent(requireContext(), HomeActivity::class.java))
                 requireActivity().finish()
             } else {
                 findNavController().navigate(R.id.action_splashFragment_to_languageFragment)
             }
//             val intent = Intent(requireContext(), HomeActivity::class.java).apply {
//                 flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//             }
//             startActivity(intent)
         }

    }
}
