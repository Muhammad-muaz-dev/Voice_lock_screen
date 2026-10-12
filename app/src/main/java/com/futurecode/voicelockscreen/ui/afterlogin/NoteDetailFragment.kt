package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentNoteDetailBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import java.text.DateFormat
import java.util.Date

class NoteDetailFragment : BaseFragment<FragmentNoteDetailBinding>(FragmentNoteDetailBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val note = VoiceLockFeatureStore.notes(requireContext()).firstOrNull { it.id == arguments?.getLong("note_id") }
        if (note == null) { findNavController().popBackStack(); return }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvTitle.text = note.title
        binding.tvMetadata.text = "${DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(note.createdAt))} · ${note.durationSec / 60}:${(note.durationSec % 60).toString().padStart(2,'0')}"
        binding.tvTranscript.text = note.transcript
        binding.tvSummary.text = note.summary
        val content = "${note.title}\n\n${note.transcript}\n\n${note.summary}"
        binding.btnCopy.setOnClickListener {
            (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(note.title,content))
            Toast.makeText(requireContext(),R.string.note_copied,Toast.LENGTH_SHORT).show()
        }
        binding.btnShare.setOnClickListener {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,content) },getString(R.string.share)))
        }
    }
}
