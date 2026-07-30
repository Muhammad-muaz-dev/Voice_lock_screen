package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentPinLockBinding
import com.futurecode.voicelockscreen.security.SecurePinStore
import com.futurecode.voicelockscreen.service.LockMonitorService

class PinLockFragment : BaseFragment<FragmentPinLockBinding>(FragmentPinLockBinding::inflate) {


    private val enteredPin = StringBuilder()
    private var pinLength = DEFAULT_PIN_LENGTH
    private var firstPin: String? = null
    private var isConfirmingPin = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pinLength = SecurePinStore.getPinLength(requireContext())
        setupKeypad()
        renderPinMode()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.tvTogglePinLength.setOnClickListener {
            pinLength = if (pinLength == 4) 6 else 4
            enteredPin.clear()
            firstPin = null
            isConfirmingPin = false
            renderPinMode()
        }

        binding.btnConfirm.setOnClickListener {
            if (enteredPin.length == pinLength) {
                handlePinCompleted()
            } else {
                Toast.makeText(requireContext(), R.string.pin_incomplete, Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnConfirm.setOnLongClickListener {
            removeLastDigit()
            true
        }
    }



    private fun setupKeypad() {
        val digitButtons = mapOf(
            binding.btnOne to "1\n  ",
            binding.btnTwo to "2\nABC",
            binding.btnThree to "3\nDEF",
            binding.btnFour to "4\nGHI",
            binding.btnFive to "5\nJKL",
            binding.btnSix to "6\nMNO",
            binding.btnSeven to "7\nPQRS",
            binding.btnEight to "8\nTUV",
            binding.btnNine to "9\nWXYZ",
            binding.btnZero to "0"
        )

        digitButtons.forEach { (button, label) ->
            button.text = formatKeyLabel(label)
            button.setOnClickListener { appendDigit(label.first().toString()) }
            button.setOnLongClickListener {
                enteredPin.clear()
                updateDots()
                true
            }
            button.disableAllCaps()
        }

        binding.btnConfirm.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN && enteredPin.isNotEmpty() && enteredPin.length < pinLength) {
                binding.btnConfirm.alpha = 0.9f
            }
            false
        }
    }

    private fun Button.disableAllCaps() {
        isAllCaps = false
    }

    private fun formatKeyLabel(label: String): SpannableString {
        val text = SpannableString(label)
        if (label.contains('\n')) {
            val letterStart = label.indexOf('\n') + 1
            text.setSpan(RelativeSizeSpan(0.34f), letterStart, label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(AbsoluteSizeSpan(7, true), letterStart, label.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return text
    }

    private fun appendDigit(digit: String) {
        if (enteredPin.length < pinLength) {
            enteredPin.append(digit)
            updateDots()
        }
    }

    private fun removeLastDigit() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.lastIndex)
            updateDots()
        }
    }

    private fun renderPinMode() {
        binding.tvCreatePin.text = getString(
            if (isConfirmingPin) R.string.confirm_your_pin else R.string.create_your_pin
        )
        binding.tvPinHint.text = getString(
            if (isConfirmingPin) R.string.pin_confirm_hint else R.string.pin_hint,
            pinLength
        )
        binding.tvTogglePinLength.text = getString(R.string.use_digit_pin, if (pinLength == 4) 6 else 4)
        setupDots()
        updateDots()
    }

    private fun setupDots() {
        binding.pinDotsContainer.removeAllViews()
        repeat(pinLength) {
            val dot = View(requireContext())
            val size = 15.dp
            val margin = 8.dp
            binding.pinDotsContainer.addView(dot, ViewGroup.MarginLayoutParams(size, size).apply {
                marginStart = margin
                marginEnd = margin
            })
        }
    }

    private fun updateDots() {
        binding.pinDotsContainer.children.forEachIndexed { index, dot ->
            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (index < enteredPin.length) R.drawable.bg_pin_dot_filled else R.drawable.bg_pin_dot_empty
            )
        }
        val isComplete = enteredPin.length == pinLength
        binding.btnConfirm.isEnabled = true
        binding.btnConfirm.alpha = if (isComplete) 1f else 0.78f
        binding.btnConfirm.setBackgroundResource(
            if (isComplete) R.drawable.bg_pin_confirm else R.drawable.bg_pin_confirm_disabled
        )
    }

    private fun handlePinCompleted() {
        val pin = enteredPin.toString()
        if (!isConfirmingPin) {
            firstPin = pin
            isConfirmingPin = true
            enteredPin.clear()
            renderPinMode()
            return
        }

        if (pin != firstPin) {
            Toast.makeText(requireContext(), R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
            firstPin = null
            isConfirmingPin = false
            enteredPin.clear()
            renderPinMode()
            return
        }

        SecurePinStore.savePin(requireContext(), pin)
        LockMonitorService.startIfNeeded(requireContext())
        Toast.makeText(requireContext(), R.string.pin_saved, Toast.LENGTH_SHORT).show()
        findNavController().popBackStack()
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val DEFAULT_PIN_LENGTH = 4
    }
}
