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
import com.futurecode.voicelockscreen.databinding.FragmentSecurityQuestionBinding
import com.futurecode.voicelockscreen.databinding.ItemSecurityQuestionBinding
import com.futurecode.voicelockscreen.security.SecureVoiceStore
import com.futurecode.voicelockscreen.utils.VoiceLockSwitchStore

class SecurityQuestionFragment : BaseFragment<FragmentSecurityQuestionBinding>(FragmentSecurityQuestionBinding::inflate) {
    private var question: String? = null
    private var answering = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = requireContext().getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, 0)
        question = savedInstanceState?.getString("question") ?: prefs.getString("security_question", null)
        answering = savedInstanceState?.getBoolean("answering") ?: false
        binding.btnBack.setOnClickListener { if (answering) { answering = false; render() } else findNavController().popBackStack() }
        binding.rvQuestions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuestions.adapter = Questions(resources.getStringArray(R.array.security_questions).toList()) {
            question = it; render()
        }
        binding.etAnswer.setText(savedInstanceState?.getString("answer") ?: prefs.getString("security_answer", ""))
        binding.etAnswer.doAfterTextChanged { binding.tvError.isVisible = false }
        binding.btnAnother.setOnClickListener { answering = false; render() }
        binding.btnSave.setOnClickListener {
            if (!answering) { answering = true; render(); return@setOnClickListener }
            val answer = binding.etAnswer.text.toString().trim()
            if (answer.length < 3) { binding.tvError.isVisible = true; return@setOnClickListener }
            prefs.edit().putString("security_question", question).putString("security_answer", answer)
                .putBoolean("security_ready", true).apply()
            if (arguments?.getBoolean("editing") == true || !SecureVoiceStore.hasPhrase(requireContext())) {
                findNavController().popBackStack()
            } else if (VoiceLockSwitchStore.hasRequiredPermissions(requireContext())) {
                findNavController().navigate(R.id.setupCompleteFragment)
            } else {
                findNavController().navigate(R.id.permissionFragment, Bundle().apply {
                    putString(PermissionFragment.ARG_TARGET, PermissionFragment.TARGET_COMPLETE)
                })
            }
        }
        render()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("question",question); outState.putBoolean("answering",answering)
        outState.putString("answer",bindingOrNull?.etAnswer?.text?.toString())
    }
    private fun render() {
        binding.rvQuestions.isVisible = !answering
        binding.answerCard.isVisible = answering
        binding.tvQuestion.text = question
        binding.tvError.isVisible = false
        binding.btnSave.setText(if (answering) R.string.save_finish else R.string.continue_text)
        binding.btnSave.isEnabled = question != null
        binding.btnSave.alpha = if (question == null) .45f else 1f
        binding.rvQuestions.adapter?.notifyDataSetChanged()
    }
    private inner class Questions(val items: List<String>, val select: (String) -> Unit) : RecyclerView.Adapter<Questions.Holder>() {
        inner class Holder(val row: ItemSecurityQuestionBinding) : RecyclerView.ViewHolder(row.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemSecurityQuestionBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val selected = items[position] == question
            holder.row.tvQuestion.text = items[position]
            holder.row.imgRadio.setImageResource(if (selected) R.drawable.ic_question_radio_on else R.drawable.ic_question_radio_off)
            // Light rows (reference style): neutral10 surface, violet stroke when picked.
            holder.row.root.setBackgroundResource(if (selected) R.drawable.vn_bg_radius_16_stroke_primary01 else R.drawable.vn_bg_radius_16_neutral10)
            holder.row.root.setOnClickListener { select(items[position]) }
        }
    }
}
