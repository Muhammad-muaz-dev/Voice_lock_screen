package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentCustomThemeBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomThemeFragment : BaseFragment<FragmentCustomThemeBinding>(FragmentCustomThemeBinding::inflate) {
    private var uri: Uri? = null
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching { requireContext().contentResolver.takePersistableUriPermission(picked,Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            uri = picked; showImage()
        } else if (uri == null) findNavController().popBackStack()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view,savedInstanceState)
        uri = savedInstanceState?.getString("uri")?.let(Uri::parse) ?: VoiceLockFeatureStore.customThemeUri(requireContext())
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnChange.setOnClickListener { picker.launch(arrayOf("image/*")) }
        binding.clock.text = SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date())
        binding.quote.text = VoiceLockFeatureStore.lockQuote(requireContext())
        binding.showClock.isChecked = savedInstanceState?.getBoolean("show") ?: VoiceLockFeatureStore.showClockAndQuote(requireContext())
        binding.showClock.setOnCheckedChangeListener { _, show -> binding.clock.isVisible=show; binding.quote.isVisible=show }
        binding.clock.isVisible=binding.showClock.isChecked; binding.quote.isVisible=binding.showClock.isChecked
        binding.btnApply.setOnClickListener {
            if (uri == null) return@setOnClickListener
            VoiceLockFeatureStore.saveCustomThemeUri(requireContext(),uri)
            VoiceLockFeatureStore.setShowClockAndQuote(requireContext(),binding.showClock.isChecked)
            Toast.makeText(requireContext(),R.string.theme_applied,Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
        if (uri == null && savedInstanceState == null) picker.launch(arrayOf("image/*")) else showImage()
    }
    private fun showImage() {
        runCatching { binding.image.setImageURI(uri) }.onFailure {
            uri=null; Toast.makeText(requireContext(),R.string.picture_unavailable,Toast.LENGTH_SHORT).show()
        }
        binding.btnApply.isEnabled = uri != null
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("uri",uri?.toString()); outState.putBoolean("show",bindingOrNull?.showClock?.isChecked ?: true)
    }
}
