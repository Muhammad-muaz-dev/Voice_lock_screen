package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentVoiceNoteBinding
import com.futurecode.voicelockscreen.databinding.ItemVoiceNoteBinding
import com.futurecode.voicelockscreen.databinding.VnPopupNoteItemBinding
import com.futurecode.voicelockscreen.databinding.VnTooltipSummaryBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceNoteItem
import com.futurecode.voicelockscreen.utils.bindBottomTabs

/** AI Voice Notes list (reference copy). */
class VoiceNoteFragment : BaseFragment<FragmentVoiceNoteBinding>(FragmentVoiceNoteBinding::inflate) {

    private var popup: PopupWindow? = null
    private var tooltip: PopupWindow? = null
    private val adapter = Notes(
        open = { openDetail(it, NoteDetailFragment.TAB_TRANSCRIPT, edit = false) },
        summary = { openDetail(it, NoteDetailFragment.TAB_SUMMARY, edit = false) },
        more = { note, anchor -> showItemMenu(note, anchor) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        VoiceNoteUi.lightStatusBar(this)
        bindBottomTabs(binding.bottomNav, 2)
        setupList()
        setupActions()
    }

    override fun onResume() {
        super.onResume()
        renderNotes()
    }

    private fun setupList() {
        binding.rvNotes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNotes.addItemDecoration(VoiceNoteUi.SpaceDecoration(resources.getDimensionPixelSize(R.dimen.vn_dp_8)))
        binding.rvNotes.adapter = adapter
    }

    private fun setupActions() {
        binding.ivSettings.setOnClickListener { findNavController().navigate(R.id.settingFragment) }
        binding.ivPremium.setOnClickListener { findNavController().navigate(R.id.premiumFragment) }
        binding.tvCreateNew.setOnClickListener { createNote() }
        binding.tvCreateNow.setOnClickListener { createNote() }
    }

    private fun renderNotes() {
        val notes = VoiceLockFeatureStore.notes(requireContext())
        adapter.submit(notes)
        binding.groupNotes.isVisible = notes.isNotEmpty()
        binding.groupEmpty.isVisible = notes.isEmpty()
        if (notes.isNotEmpty()) maybeShowSummaryTip()
    }

    private fun createNote() {
        // Existing free-tries rule is kept; the reference's ad-based gating is not copied.
        if (!prefManager.isPro && VoiceLockFeatureStore.remainingTries(requireContext()) <= 0) {
            findNavController().navigate(R.id.premiumFragment)
            return
        }
        findNavController().navigate(R.id.noteRecordFragment, null, VoiceNoteUi.slideNavOptions)
    }

    private fun openDetail(note: VoiceNoteItem, tab: Int, edit: Boolean) {
        dismissPopups()
        val args = Bundle().apply {
            putLong(NoteDetailFragment.ARG_NOTE_ID, note.id)
            putInt(NoteDetailFragment.ARG_TAB, tab)
            putBoolean(NoteDetailFragment.ARG_EDIT, edit)
        }
        findNavController().navigate(R.id.noteDetailFragment, args, VoiceNoteUi.slideNavOptions)
    }

    private fun showItemMenu(note: VoiceNoteItem, anchor: View) {
        dismissPopups()
        val menu = VnPopupNoteItemBinding.inflate(layoutInflater)
        val window = VoiceNoteUi.showPopup(anchor, menu.root)
        popup = window
        menu.frameEdit.setOnClickListener { window.dismiss(); openDetail(note, NoteDetailFragment.TAB_TRANSCRIPT, edit = true) }
        menu.frameSummary.setOnClickListener { window.dismiss(); openDetail(note, NoteDetailFragment.TAB_SUMMARY, edit = false) }
        menu.frameDelete.setOnClickListener {
            window.dismiss()
            VoiceNoteUi.showDeleteNote(requireContext()) {
                val context = context ?: return@showDeleteNote
                VoiceLockFeatureStore.deleteNote(context, note.id)
                VoiceNoteUi.toast(context, R.string.vn_deleted_successfully)
                if (bindingOrNull != null) renderNotes()
            }
        }
    }

    /** One-time coach bubble pointing at the first note's AI summary chip. */
    private fun maybeShowSummaryTip() {
        if (VoiceLockFeatureStore.isSummaryTipShown(requireContext())) return
        binding.rvNotes.doOnLayout {
            val row = bindingOrNull?.rvNotes?.findViewHolderForAdapterPosition(0) as? Notes.Holder ?: return@doOnLayout
            if (tooltip?.isShowing == true || !isResumed) return@doOnLayout
            val anchor = row.row.ivSummary
            val tip = VnTooltipSummaryBinding.inflate(layoutInflater)
            tip.root.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val tipWidth = tip.root.measuredWidth
            val arrowWidth = resources.getDimensionPixelSize(R.dimen.vn_dp_14)
            val window = PopupWindow(tip.root, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false)
            window.isOutsideTouchable = true
            val anchorLoc = IntArray(2).also { anchor.getLocationInWindow(it) }
            val maxX = binding.root.width - tipWidth
            val x = (anchorLoc[0] + anchor.width / 2 - tipWidth + arrowWidth * 2).coerceIn(0, maxOf(0, maxX))
            tip.ivArrow.translationX = (anchorLoc[0] + anchor.width / 2 - arrowWidth / 2 - x).toFloat()
            val y = anchorLoc[1] - tip.root.measuredHeight
            tip.root.setOnClickListener { window.dismiss() }
            window.setOnDismissListener { context?.let { VoiceLockFeatureStore.setSummaryTipShown(it) } }
            runCatching { window.showAtLocation(binding.root, android.view.Gravity.NO_GRAVITY, x, y) }
            tooltip = window
        }
    }

    private fun dismissPopups() {
        popup?.dismiss(); popup = null
        tooltip?.dismiss(); tooltip = null
    }

    override fun onPause() {
        dismissPopups()
        super.onPause()
    }

    private class Notes(
        val open: (VoiceNoteItem) -> Unit,
        val summary: (VoiceNoteItem) -> Unit,
        val more: (VoiceNoteItem, View) -> Unit
    ) : RecyclerView.Adapter<Notes.Holder>() {
        private var items: List<VoiceNoteItem> = emptyList()

        class Holder(val row: ItemVoiceNoteBinding) : RecyclerView.ViewHolder(row.root)

        fun submit(list: List<VoiceNoteItem>) {
            items = list
            @Suppress("NotifyDataSetChanged")
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemVoiceNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val note = items[position]
            holder.row.tvName.text = note.title
            holder.row.tvDate.text = VoiceNoteUi.formatDate(holder.itemView.context, note.createdAt)
            holder.row.root.setOnClickListener { open(note) }
            holder.row.ivSummary.setOnClickListener { summary(note) }
            holder.row.ivSetting.setOnClickListener { more(note, it) }
        }
    }
}
