package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentScreenPreviewBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenPreviewFragment : BaseFragment<FragmentScreenPreviewBinding>(FragmentScreenPreviewBinding::inflate) {


    private val methods = listOf(
        PreviewMethod("PIN", R.drawable.ic_pin),
        PreviewMethod("Pattern", R.drawable.ic_pattern),
        PreviewMethod("Voice", R.drawable.ic_mic_blue),
        PreviewMethod("Fingerprint", R.drawable.ic_fingerprint)
    )
    private var selectedMethod = 2



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindDateTime()
        bindMethods()
        updateSelectedMethod()
    }



    private fun bindDateTime() {
        val now = Date()
        binding.tvStatusTime.text = SimpleDateFormat("H:mm", Locale.getDefault()).format(now)
        binding.tvMainTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        binding.tvDate.text = SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(now)
    }

    private fun bindMethods() {
        binding.methodsRow.children.forEachIndexed { index, child ->
            val method = methods[index]
            val container = child as LinearLayout
            val icon = container.findViewById<ImageView>(R.id.imgMethod)
            val label = container.findViewById<TextView>(R.id.tvMethod)
            icon.setImageResource(method.iconRes)
            label.text = method.title
            container.setOnClickListener {
                selectedMethod = index
                updateSelectedMethod()
            }
        }
    }

    private fun updateSelectedMethod() {
        binding.methodsRow.children.forEachIndexed { index, child ->
            val container = child as LinearLayout
            val icon = container.findViewById<ImageView>(R.id.imgMethod)
            val label = container.findViewById<TextView>(R.id.tvMethod)
            val selected = index == selectedMethod
            container.setBackgroundResource(
                if (selected) R.drawable.bg_preview_method_selected else R.drawable.bg_preview_method
            )
            icon.setBackgroundResource(
                if (selected) R.drawable.bg_preview_method_icon_selected else R.drawable.bg_preview_method_icon
            )
            icon.setColorFilter(
                ContextCompat.getColor(requireContext(), if (selected) R.color.blue_primary else R.color.white)
            )
            label.setTextColor(
                ContextCompat.getColor(requireContext(), if (selected) R.color.blue_primary else R.color.white)
            )
        }

        binding.tvSpeakHint.text = when (selectedMethod) {
            0 -> "Enter your PIN to unlock"
            1 -> "Draw your pattern to unlock"
            2 -> getString(R.string.tap_microphone)
            else -> "Touch fingerprint sensor"
        }
    }

    private data class PreviewMethod(
        val title: String,
        val iconRes: Int
    )
}
