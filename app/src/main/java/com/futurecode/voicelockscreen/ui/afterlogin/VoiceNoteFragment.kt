package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceNoteBinding
import com.futurecode.voicelockscreen.databinding.ItemVoiceNoteBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceNoteItem
import com.futurecode.voicelockscreen.utils.bindBottomTabs

class VoiceNoteFragment : BaseFragment<FragmentVoiceNoteBinding>(FragmentVoiceNoteBinding::inflate) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindBottomTabs(binding.bottomNav, 2)
        val notes = VoiceLockFeatureStore.notes(requireContext())
        binding.rvNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotes.adapter = Notes(notes) { note ->
            findNavController().navigate(R.id.noteDetailFragment, Bundle().apply { putLong("note_id", note.id) })
        }
        binding.emptyState.isVisible = notes.isEmpty()
        binding.rvNotes.isVisible = notes.isNotEmpty()
        binding.ivSettings.setOnClickListener { findNavController().navigate(R.id.settingFragment) }
        binding.ivPremium.setOnClickListener { findNavController().navigate(R.id.premiumFragment) }
        binding.btnGoPremium.setOnClickListener { findNavController().navigate(R.id.premiumFragment) }
        binding.btnRecord.setOnClickListener {
            if (!prefManager.isPro && VoiceLockFeatureStore.remainingTries(requireContext()) <= 0) {
                binding.paywallCard.isVisible = true
                binding.btnRecord.isVisible = false
            } else findNavController().navigate(R.id.noteRecordFragment)
        }
        binding.tvWatchAd.setText(R.string.note_free_body)
    }
    private class Notes(val items: List<VoiceNoteItem>, val open: (VoiceNoteItem) -> Unit) : RecyclerView.Adapter<Notes.Holder>() {
        class Holder(val row: ItemVoiceNoteBinding) : RecyclerView.ViewHolder(row.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemVoiceNoteBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val note = items[position]
            holder.row.tvNoteTitle.text = note.title
            holder.row.tvNoteSummary.text = note.summary
            holder.row.root.setOnClickListener { open(note) }
        }
    }
}
