package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPatternLockBinding
import com.futurecode.voicelockscreen.security.SecurePatternStore
import com.futurecode.voicelockscreen.service.LockMonitorService
class PatternLockFragment : BaseFragment<FragmentPatternLockBinding>(FragmentPatternLockBinding::inflate) {

    private var firstPattern: String? = null
    private var isConfirmingPattern = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.patternView.onPatternComplete = { sequence ->
            handlePattern(sequence)
        }
    }

    private fun handlePattern(sequence: String) {
        val dotCount = sequence.split("-").filter { it.isNotBlank() }.size
        if (dotCount < 4) {
            binding.tvPatternStatus.text = getString(R.string.pattern_too_short)
            Toast.makeText(requireContext(), R.string.pattern_too_short, Toast.LENGTH_SHORT).show()
            binding.patternView.clearPattern()
            return
        }

        if (!isConfirmingPattern) {
            firstPattern = sequence
            isConfirmingPattern = true
            binding.tvCreatePattern.text = getString(R.string.confirm_your_pattern)
            binding.tvPatternHint.text = getString(R.string.pattern_confirm_hint)
            binding.tvPatternStatus.text = ""
            binding.patternView.clearPattern()
            return
        }

        if (sequence == firstPattern) {
            SecurePatternStore.savePattern(requireContext(), sequence)
            LockMonitorService.startIfNeeded(requireContext())
            binding.tvPatternStatus.text = getString(R.string.pattern_saved)
            Toast.makeText(requireContext(), R.string.pattern_saved, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        } else {
            firstPattern = null
            isConfirmingPattern = false
            binding.tvCreatePattern.text = getString(R.string.create_your_pattern)
            binding.tvPatternHint.text = getString(R.string.pattern_hint)
            binding.tvPatternStatus.text = getString(R.string.pattern_mismatch)
            Toast.makeText(requireContext(), R.string.pattern_mismatch, Toast.LENGTH_SHORT).show()
            binding.patternView.clearPattern()
        }
    }
}
