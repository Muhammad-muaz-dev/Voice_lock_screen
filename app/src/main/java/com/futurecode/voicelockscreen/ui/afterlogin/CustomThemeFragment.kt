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
import com.futurecode.voicelockscreen.utils.ThemePreviewBinder
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore

/** Upload Image: system picker, then the reference preview with Apply. */
class CustomThemeFragment : BaseFragment<FragmentCustomThemeBinding>(FragmentCustomThemeBinding::inflate) {
    private var uri: Uri? = null
    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) {
            runCatching { requireContext().contentResolver.takePersistableUriPermission(picked, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            uri = picked; showImage()
        } else if (uri == null) findNavController().popBackStack()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val preview = binding.preview
        uri = savedInstanceState?.getString("uri")?.let(Uri::parse)
        preview.ivBack.setOnClickListener { findNavController().popBackStack() }
        ThemePreviewBinder.bindContent(preview, requireContext(), VoiceLockFeatureStore.lockQuote(requireContext()))
        preview.btnApply.setOnClickListener {
            val picked = uri ?: return@setOnClickListener
            VoiceLockFeatureStore.saveCustomThemeUri(requireContext(), picked)
            VoiceLockFeatureStore.setShowClockAndQuote(requireContext(), true)
            Toast.makeText(requireContext(), R.string.theme_applied, Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
        if (uri == null && savedInstanceState == null) picker.launch(arrayOf("image/*")) else showImage()
    }

    private fun showImage() {
        val preview = bindingOrNull?.preview ?: return
        runCatching { preview.ivTheme.setImageURI(uri) }.onFailure {
            uri = null; Toast.makeText(requireContext(), R.string.picture_unavailable, Toast.LENGTH_SHORT).show()
        }
        preview.btnApply.isVisible = uri != null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("uri", uri?.toString())
    }
}
