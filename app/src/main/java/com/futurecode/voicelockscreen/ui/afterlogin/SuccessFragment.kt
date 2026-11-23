package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.ViewSuccessPageBinding

class SuccessFragment : BaseFragment<ViewSuccessPageBinding>(ViewSuccessPageBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = requireArguments().getInt(ARG_TITLE)
        binding.successScreen.bindSuccess(title) { findNavController().popBackStack() }
    }

    companion object {
        private const val ARG_TITLE = "successTitle"

        fun replaceCurrent(controller: NavController, @StringRes title: Int) {
            val current = controller.currentDestination ?: return
            controller.navigate(
                R.id.successFragment,
                bundleOf(ARG_TITLE to title),
                NavOptions.Builder().setPopUpTo(current.id, true).build()
            )
        }
    }
}
