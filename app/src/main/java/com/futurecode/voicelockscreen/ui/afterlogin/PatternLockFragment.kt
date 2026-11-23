package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPatternLockBinding
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.service.LockService

class PatternLockFragment : BaseFragment<FragmentPatternLockBinding>(FragmentPatternLockBinding::inflate) {

    private var firstPattern: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ivBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnStartOver.setOnClickListener {
            firstPattern = null
            binding.patternView.clearPattern()
            renderPrompt()
        }
        binding.patternView.useSetupAppearance()
        binding.patternView.onPatternComplete = ::handlePattern
        renderPrompt()
    }

    private fun handlePattern(sequence: String) {
        binding.patternView.clearPattern()
        if (sequence.count { it == '-' } < 3) {
            renderPrompt(R.string.pattern_too_short)
            return
        }

        val original = firstPattern
        when {
            original == null -> {
                firstPattern = sequence
                renderPrompt()
            }
            sequence != original -> renderPrompt(R.string.pattern_setup_retry)
            else -> {
                SecurePatternStore.savePattern(requireContext(), sequence)
                LockService.enableAndStart(requireContext())
                showSetupSuccess(R.string.pattern_saved) { findNavController().popBackStack() }
            }
        }
    }

    private fun renderPrompt(error: Int? = null) {
        val confirming = firstPattern != null
        binding.btnStartOver.isVisible = confirming
        binding.tvCreatePattern.setText(
            error ?: if (confirming) R.string.pattern_setup_confirm else R.string.pattern_setup_draw
        )
        binding.tvCreatePattern.setTextColor(
            ContextCompat.getColor(requireContext(), if (error == null) R.color.textPrimary else R.color.error)
        )
    }
}
