package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.BottomSheetRecoveryQuestionBinding
import com.futurecode.voicelockscreen.databinding.FragmentSecurityQuestionBinding
import com.futurecode.voicelockscreen.databinding.ItemRecoveryQuestionBinding
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Recovery Password: choose a question from a bottom sheet, then save an answer. */
class SecurityQuestionFragment :
    BaseFragment<FragmentSecurityQuestionBinding>(FragmentSecurityQuestionBinding::inflate) {

    private var question: String? = null
    private var sheet: BottomSheetDialog? = null

    private val prefs by lazy {
        requireContext().getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        question = savedInstanceState?.getString(KEY_QUESTION) ?: prefs.getString(PREF_QUESTION, null)
        binding.etAnswer.setText(savedInstanceState?.getString(KEY_ANSWER) ?: prefs.getString(PREF_ANSWER, ""))
        setupActions()
        render()
    }

    override fun onDestroyView() {
        sheet?.dismiss()
        sheet = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_QUESTION, question)
        outState.putString(KEY_ANSWER, bindingOrNull?.etAnswer?.text?.toString())
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.questionField.setOnClickListener { showQuestionSheet() }
        binding.etAnswer.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.btnSave.setOnClickListener { save() }
    }

    private fun render() {
        binding.tvQuestion.text = question
        val ready = question != null
        binding.questionField.setText(
            if (ready) R.string.choose_another_security_question else R.string.choose_security_question
        )
        binding.btnSave.isEnabled = ready
        binding.btnSave.alpha = if (ready) 1f else DISABLED_ALPHA
    }

    private fun save() {
        val answer = binding.etAnswer.text.toString().trim()
        if (question == null) {
            showQuestionSheet()
            return
        }
        if (answer.length < MIN_ANSWER) {
            binding.tvError.isVisible = true
            return
        }
        prefs.edit()
            .putString(PREF_QUESTION, question)
            .putString(PREF_ANSWER, answer)
            .putBoolean(PREF_READY, true)
            .apply()
        navigateAfterSave()
    }

    private fun navigateAfterSave() {
        val context = requireContext()
        when {
            arguments?.getBoolean(ARG_EDITING) == true || !SecureVoiceStore.hasPhrase(context) ->
                showSetupSuccess(R.string.recovery_question_saved) { findNavController().popBackStack() }
            VoiceLockSwitchStore.hasRequiredPermissions(context) ->
                completeVoiceSetup()
            else -> findNavController().navigate(R.id.permissionFragment, Bundle().apply {
                putString(PermissionFragment.ARG_TARGET, PermissionFragment.TARGET_COMPLETE)
            })
        }
    }

    // region Question sheet

    private fun showQuestionSheet() {
        if (sheet?.isShowing == true) return
        val items = resources.getStringArray(R.array.security_questions).toList()
        var pending = question
        val sheetBinding = BottomSheetRecoveryQuestionBinding.inflate(layoutInflater)
        val dialog = VoiceNoteUi.transparentSheet(
            context = requireContext(),
            root = sheetBinding.root,
            fullScreen = true
        )

        val adapter = QuestionAdapter(items, { pending }) { picked -> pending = picked }
        sheetBinding.rvSheetQuestions.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.rvSheetQuestions.adapter = adapter
        sheetBinding.etSearch.doAfterTextChanged { text ->
            val query = text?.toString()?.trim().orEmpty()
            val filteredItems = items.filter { it.contains(query, ignoreCase = true) }
            adapter.submit(filteredItems)
            sheetBinding.rvSheetQuestions.isVisible = filteredItems.isNotEmpty()
            sheetBinding.tvNoQuestionsFound.isVisible = filteredItems.isEmpty()
        }
        sheetBinding.btnSheetClose.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnSheetApply.setOnClickListener {
            if (pending != null) {
                question = pending
                render()
            }
            dialog.dismiss()
        }
        dialog.setOnDismissListener { sheet = null }
        sheet = dialog
        dialog.show()
        com.futurecode.voicelockscreen.utils.SystemBars.hide(dialog)
    }

    private class QuestionAdapter(
        initialItems: List<String>,
        private val selected: () -> String?,
        private val onPick: (String) -> Unit
    ) : RecyclerView.Adapter<QuestionAdapter.Holder>() {

        private var items = initialItems

        class Holder(val row: ItemRecoveryQuestionBinding) : RecyclerView.ViewHolder(row.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemRecoveryQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun getItemCount() = items.size

        fun submit(items: List<String>) {
            this.items = items
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val item = items[position]
            val isSelected = item == selected()
            holder.row.tvQuestion.text = item
            holder.row.ivRadio.setImageResource(if (isSelected) R.drawable.ic_radio_on_ring else R.drawable.ic_radio_off_ring)
            holder.row.root.setBackgroundResource(
                if (isSelected) R.drawable.vn_bg_radius_16_stroke_primary01
                else R.drawable.vn_bg_radius_16_neutral10
            )
            holder.row.root.isSelected = isSelected
            holder.row.root.setOnClickListener {
                onPick(item)
                notifyDataSetChanged()
            }
        }
    }

    // endregion

    internal companion object {
        private const val KEY_QUESTION = "question"
        private const val KEY_ANSWER = "answer"
        const val PREF_QUESTION = "security_question"
        const val PREF_ANSWER = "security_answer"
        private const val PREF_READY = "security_ready"
        private const val ARG_EDITING = "editing"
        private const val MIN_ANSWER = 3
        private const val DISABLED_ALPHA = 0.45f
    }
}
