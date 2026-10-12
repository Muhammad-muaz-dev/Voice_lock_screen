package com.futurecode.voicelockscreen.ui.afterlogin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentAiQuoteBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore

class AiQuoteFragment : BaseFragment<FragmentAiQuoteBinding>(FragmentAiQuoteBinding::inflate) {
    private val topics = listOf("Passion", "Growth", "Vitality", "Aspire", "Wisdom", "Courage", "Harmony", "Gratitude", "Dreams", "Hope")
    private var topic = ""
    private var result = ""
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topic = savedInstanceState?.getString("topic").orEmpty()
        result = savedInstanceState?.getString("result").orEmpty()
        binding.etQuote.setText(savedInstanceState?.getString("input").orEmpty())
        binding.btnBack.setOnClickListener { if (result.isNotBlank()) { result = ""; render() } else findNavController().popBackStack() }
        binding.etQuote.doAfterTextChanged { updateCount() }
        binding.rvTopics.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTopics.adapter = Topics()
        binding.btnAnother.setOnClickListener { result = ""; render() }
        binding.btnUseQuote.setOnClickListener {
            if (result.isNotBlank()) {
                VoiceLockFeatureStore.saveLockQuote(requireContext(),result)
                findNavController().popBackStack()
            } else {
                result = binding.etQuote.text.toString().trim().ifBlank { VoiceLockFeatureStore.quoteForTopic(topic) }
                render()
            }
        }
        render()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("topic",topic); outState.putString("result",result)
        outState.putString("input",bindingOrNull?.etQuote?.text?.toString())
    }
    private fun render() {
        val showingResult = result.isNotBlank()
        binding.editor.isVisible = !showingResult; binding.result.isVisible = showingResult
        binding.btnAnother.isVisible = showingResult
        binding.tvResult.text = "“$result”"
        binding.tvTopic.text = if (topic.isBlank()) "" else getString(R.string.quote_topic,topic)
        binding.btnUseQuote.setText(if (showingResult) R.string.use_on_lock_screen else R.string.quote_make)
        updateCount()
    }
    private fun updateCount() {
        binding.tvCount.text = "${binding.etQuote.text?.length ?: 0}/80"
        binding.btnUseQuote.isEnabled = result.isNotBlank() || topic.isNotBlank() || !binding.etQuote.text.isNullOrBlank()
        binding.btnUseQuote.alpha = if (binding.btnUseQuote.isEnabled) 1f else .45f
    }
    private inner class Topics : RecyclerView.Adapter<Topics.Holder>() {
        inner class Holder(val view: TextView) : RecyclerView.ViewHolder(view)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_topic,parent,false) as TextView)
        override fun getItemCount() = topics.size
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val name = topics[position]
            holder.view.text = name
            holder.view.setBackgroundResource(if (topic == name) R.drawable.bg_row_language_selected else R.drawable.bg_row_language)
            holder.view.setOnClickListener { topic = name; notifyDataSetChanged(); updateCount() }
        }
    }
}
