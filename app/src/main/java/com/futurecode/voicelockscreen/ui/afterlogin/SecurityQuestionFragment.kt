package com.futurecode.voicelockscreen.ui.afterlogin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.BottomSheetSecurityQuestionsBinding
import com.futurecode.voicelockscreen.databinding.FragmentSecurityQuestionBinding
import com.futurecode.voicelockscreen.databinding.ItemSecurityQuestionBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class SecurityQuestionFragment : BaseFragment<FragmentSecurityQuestionBinding>(FragmentSecurityQuestionBinding::inflate) {


    private val questions = listOf(
        "What was the name of your primary school?",
        "What is the name of your first pet?",
        "What was the name of the street you grew up on?",
        "What is your mother's maiden name?",
        "What city were you born in?",
        "What was the make of your first car?",
        "What is the name of your oldest sibling?",
        "What was your childhood nickname?"
    )
    private var selectedQuestion: String? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.header.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.tvQuestion.setOnClickListener { showQuestionSheet() }
        binding.btnSave.setOnClickListener { saveSecurityQuestion() }

        val prefs = requireContext().getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, Context.MODE_PRIVATE)
        selectedQuestion = prefs.getString(KEY_SECURITY_QUESTION, null)
        selectedQuestion?.let { binding.tvQuestion.text = it }
        binding.etAnswer.setText(prefs.getString(KEY_SECURITY_ANSWER, ""))
    }



    private fun showQuestionSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetSecurityQuestionsBinding.inflate(layoutInflater)
        var pendingQuestion = selectedQuestion ?: questions.first()
        val adapter = QuestionAdapter(questions, pendingQuestion) {
            pendingQuestion = it
        }

        sheetBinding.rvQuestions.layoutManager = LinearLayoutManager(requireContext())
        sheetBinding.rvQuestions.adapter = adapter
        sheetBinding.btnConfirmQuestion.setOnClickListener {
            selectedQuestion = pendingQuestion
            binding.tvQuestion.text = pendingQuestion
            binding.tvQuestion.setTextColor(0xFF172238.toInt())
            dialog.dismiss()
        }
        dialog.setContentView(sheetBinding.root)
        dialog.show()
    }

    private fun saveSecurityQuestion() {
        val question = selectedQuestion
        val answer = binding.etAnswer.text?.toString()?.trim().orEmpty()
        when {
            question.isNullOrBlank() -> Toast.makeText(requireContext(), R.string.choose_question_error, Toast.LENGTH_SHORT).show()
            answer.isBlank() -> Toast.makeText(requireContext(), R.string.answer_required, Toast.LENGTH_SHORT).show()
            else -> {
                requireContext()
                    .getSharedPreferences(VoiceSetupFragment.VOICE_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SECURITY_QUESTION, question)
                    .putString(KEY_SECURITY_ANSWER, answer)
                    .putBoolean(KEY_SECURITY_READY, true)
                    .apply()
                Toast.makeText(requireContext(), R.string.security_saved, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
    }

    private class QuestionAdapter(
        private val questions: List<String>,
        selectedQuestion: String,
        private val onSelected: (String) -> Unit
    ) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

        private var selectedIndex = questions.indexOf(selectedQuestion).coerceAtLeast(0)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
            val binding = ItemSecurityQuestionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return QuestionViewHolder(binding)
        }

        override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
            holder.bind(questions[position], position == selectedIndex) {
                val oldIndex = selectedIndex
                selectedIndex = position
                notifyItemChanged(oldIndex)
                notifyItemChanged(selectedIndex)
                onSelected(questions[position])
            }
        }

        override fun getItemCount(): Int = questions.size

        private class QuestionViewHolder(
            private val binding: ItemSecurityQuestionBinding
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(question: String, isSelected: Boolean, onClick: () -> Unit) {
                binding.tvQuestion.text = question
                binding.imgRadio.setImageResource(
                    if (isSelected) R.drawable.ic_question_radio_on else R.drawable.ic_question_radio_off
                )
                binding.root.setOnClickListener { onClick() }
            }
        }
    }

    companion object {
        private const val KEY_SECURITY_QUESTION = "security_question"
        private const val KEY_SECURITY_ANSWER = "security_answer"
        private const val KEY_SECURITY_READY = "security_ready"
    }
}
