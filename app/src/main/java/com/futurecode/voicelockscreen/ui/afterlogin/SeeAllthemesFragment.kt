package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentSeeAllthemesBinding
class SeeAllthemesFragment : BaseFragment<FragmentSeeAllthemesBinding>(FragmentSeeAllthemesBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // RecyclerView setup would typically happen here
    }
}
