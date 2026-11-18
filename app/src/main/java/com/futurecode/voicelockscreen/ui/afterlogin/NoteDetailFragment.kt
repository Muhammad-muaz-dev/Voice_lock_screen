package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentNoteDetailBinding
import com.futurecode.voicelockscreen.databinding.VnPopupNoteOptionBinding
import com.futurecode.voicelockscreen.utils.NoteAiService
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore
import com.futurecode.voicelockscreen.utils.VoiceNoteItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Note detail (reference copy): Transcript / AI Summary / Translate tabs, edit mode, option menu. */
class NoteDetailFragment : BaseFragment<FragmentNoteDetailBinding>(FragmentNoteDetailBinding::inflate) {

    private lateinit var note: VoiceNoteItem
    private var tab = TAB_TRANSCRIPT
    private var editing = false
    private var aiJob: Job? = null

    private val backWhileEditing = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = finishEditing()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val id = arguments?.getLong(ARG_NOTE_ID) ?: 0L
        val found = VoiceLockFeatureStore.note(requireContext(), id)
        if (found == null) {
            findNavController().popBackStack()
            return
        }
        note = found
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backWhileEditing)
        tab = savedInstanceState?.getInt(ARG_TAB) ?: arguments?.getInt(ARG_TAB, TAB_TRANSCRIPT) ?: TAB_TRANSCRIPT
        binding.etNote.setText(note.transcript)
        binding.llLoadingOverlay.isClickable = true
        setupActions()
        render()
        if (savedInstanceState == null && arguments?.getBoolean(ARG_EDIT, false) == true) startEditing()
    }

    private fun setupActions() {
        binding.ivBack.setOnClickListener {
            if (editing) finishEditing()
            findNavController().popBackStack()
        }
        binding.tvNoteName.setOnClickListener {
            VoiceNoteUi.showNoteName(requireContext(), note.title) { name ->
                persist(note.copy(title = name))
            }
        }
        binding.ivOption.setOnClickListener { showOptions(it) }
        binding.tvDone.setOnClickListener { finishEditing() }
        binding.tvTranscript.setOnClickListener { selectTab(TAB_TRANSCRIPT) }
        binding.tvAISummary.setOnClickListener { selectTab(TAB_SUMMARY) }
        binding.tvTranslate.setOnClickListener { selectTab(TAB_TRANSLATE) }
        binding.tvSummarize.setOnClickListener { summarize() }
        binding.tvRegenerateSummary.setOnClickListener { summarize() }
        binding.tvLanguage.setOnClickListener { pickLanguage(translateAfter = false) }
        binding.tvLanguageTranslate.setOnClickListener { pickLanguage(translateAfter = true) }
        binding.btnTranslate.setOnClickListener { translate() }
        binding.etNote.setOnLongClickListener { if (!editing) copy(note.transcript) else false }
        binding.tvSummaryContent.setOnLongClickListener { copy(note.summary) }
        binding.tvTranslateContent.setOnLongClickListener { copy(note.translation) }
    }

    private fun selectTab(newTab: Int) {
        if (editing && newTab != TAB_TRANSCRIPT) finishEditing()
        tab = newTab
        render()
    }

    private fun render() {
        val b = bindingOrNull ?: return
        b.tvNoteName.text = note.title
        styleTab(b.tvTranscript, tab == TAB_TRANSCRIPT)
        styleTab(b.tvAISummary, tab == TAB_SUMMARY)
        styleTab(b.tvTranslate, tab == TAB_TRANSLATE)
        b.ivOption.isVisible = !editing
        b.tvDone.isVisible = editing

        b.etNote.isVisible = tab == TAB_TRANSCRIPT
        b.groupSummaryEmpty.isVisible = tab == TAB_SUMMARY && note.summary.isBlank()
        b.groupSummaryContent.isVisible = tab == TAB_SUMMARY && note.summary.isNotBlank()
        b.tvSummaryContent.text = note.summary

        val tag = note.translateLanguage.ifBlank { VoiceLockFeatureStore.translateLanguage(requireContext()) }
        val hasTranslation = note.translation.isNotBlank()
        b.groupTranslateEmpty.isVisible = tab == TAB_TRANSLATE && !hasTranslation
        b.tvTranslateContent.isVisible = tab == TAB_TRANSLATE && hasTranslation
        b.tvTranslateContent.text = note.translation
        b.tvLanguage.text = VoiceNoteUi.languageLabel(VoiceLockFeatureStore.translateLanguage(requireContext()))
        b.tvLanguageTranslate.isVisible = hasTranslation
        b.tvLanguageTranslate.text = VoiceNoteUi.languageLabel(tag)
    }

    private fun styleTab(view: TextView, selected: Boolean) {
        view.isSelected = selected
        VoiceNoteUi.setBackgroundKeepPadding(
            view,
            if (selected) R.drawable.vn_bg_radius_1000_primary01 else R.drawable.vn_bg_radius_1000_stroke_primary01
        )
        view.setTextColor(ContextCompat.getColor(view.context, if (selected) R.color.vn_white else R.color.vn_primary01))
    }

    private fun startEditing() {
        tab = TAB_TRANSCRIPT
        editing = true
        backWhileEditing.isEnabled = true
        binding.etNote.isEnabled = true
        binding.etNote.setSelection(binding.etNote.text?.length ?: 0)
        VoiceNoteUi.showKeyboard(binding.etNote)
        render()
    }

    private fun finishEditing() {
        val b = bindingOrNull ?: return
        if (!editing) return
        editing = false
        backWhileEditing.isEnabled = false
        b.etNote.isEnabled = false
        VoiceNoteUi.hideKeyboard(b.etNote)
        val text = b.etNote.text?.toString()?.trim().orEmpty()
        if (text != note.transcript) persist(note.copy(transcript = text)) else render()
    }

    private fun showOptions(anchor: View) {
        val menu = VnPopupNoteOptionBinding.inflate(layoutInflater)
        val window = VoiceNoteUi.showPopup(anchor, menu.root)
        menu.llEdit.setOnClickListener {
            window.dismiss()
            startEditing()
        }
        menu.llDelete.setOnClickListener {
            window.dismiss()
            VoiceNoteUi.showDeleteNote(requireContext()) {
                val context = context ?: return@showDeleteNote
                VoiceLockFeatureStore.deleteNote(context, note.id)
                VoiceNoteUi.toast(context, R.string.vn_deleted_successfully)
                if (bindingOrNull != null) findNavController().popBackStack()
            }
        }
    }

    private fun summarize() {
        if (aiJob?.isActive == true) return
        if (note.transcript.trim().length < NoteAiService.MIN_SUMMARY_CHARS) {
            VoiceNoteUi.toast(requireContext(), R.string.vn_note_too_short, R.drawable.vn_ic_logo_voice_note)
            return
        }
        runAi(R.string.vn_ai_is_summarizing, R.string.vn_summary_failed) {
            NoteAiService.instance.summarize(note.transcript).map { note.copy(summary = it) }
        }
    }

    private fun translate(tag: String = VoiceLockFeatureStore.translateLanguage(requireContext())) {
        if (aiJob?.isActive == true) return
        if (note.transcript.isBlank()) {
            VoiceNoteUi.toast(requireContext(), R.string.vn_note_too_short, R.drawable.vn_ic_logo_voice_note)
            return
        }
        runAi(R.string.vn_translating_your_note, R.string.vn_translation_not_available) {
            NoteAiService.instance.translate(note.transcript, tag).map { note.copy(translation = it, translateLanguage = tag) }
        }
    }

    private fun runAi(loading: Int, failure: Int, call: suspend () -> Result<VoiceNoteItem>) {
        showLoading(loading)
        aiJob = viewLifecycleOwner.lifecycleScope.launch {
            val result = runCatching { call() }.getOrElse { Result.failure(it) }
            hideLoading()
            val context = context ?: return@launch
            result
                .onSuccess { persist(it) }
                .onFailure { VoiceNoteUi.errorToast(context, failure) }
        }
    }

    private fun pickLanguage(translateAfter: Boolean) {
        val current = VoiceLockFeatureStore.translateLanguage(requireContext())
        VoiceNoteUi.showTranslateLanguages(requireContext(), current) { tag ->
            val context = context ?: return@showTranslateLanguages
            VoiceLockFeatureStore.saveTranslateLanguage(context, tag)
            render()
            if (translateAfter && tag != note.translateLanguage) translate(tag)
        }
    }

    private fun showLoading(message: Int) {
        binding.tvLoading.setText(message)
        binding.llLoadingOverlay.isVisible = true
        binding.llLoadingOverlay.alpha = 0f
        binding.llLoadingOverlay.animate().alpha(1f).setDuration(FADE_MS).start()
    }

    private fun hideLoading() {
        val overlay = bindingOrNull?.llLoadingOverlay ?: return
        overlay.animate().alpha(0f).setDuration(FADE_MS).withEndAction { overlay.isVisible = false }.start()
    }

    private fun persist(updated: VoiceNoteItem) {
        note = updated
        context?.let { VoiceLockFeatureStore.updateNote(it, updated) }
        render()
    }

    private fun copy(text: String): Boolean {
        if (text.isBlank()) return false
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(note.title, text))
        VoiceNoteUi.toast(requireContext(), R.string.vn_text_copied)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_TAB, tab)
    }

    override fun onPause() {
        // Keep edits if the app goes to the background mid-edit.
        if (editing && ::note.isInitialized) {
            val text = bindingOrNull?.etNote?.text?.toString()?.trim()
            if (text != null && text != note.transcript) {
                note = note.copy(transcript = text)
                context?.let { VoiceLockFeatureStore.updateNote(it, note) }
            }
        }
        super.onPause()
    }

    override fun onDestroyView() {
        aiJob?.cancel()
        super.onDestroyView()
    }

    companion object {
        const val ARG_NOTE_ID = "note_id"
        const val ARG_TAB = "tab"
        const val ARG_EDIT = "edit"
        const val TAB_TRANSCRIPT = 0
        const val TAB_SUMMARY = 1
        const val TAB_TRANSLATE = 2
        private const val FADE_MS = 200L
    }
}
