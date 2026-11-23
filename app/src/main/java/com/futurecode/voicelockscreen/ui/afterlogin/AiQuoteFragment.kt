package com.futurecode.voicelockscreen.ui.afterlogin

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentAiQuoteBinding
import com.futurecode.voicelockscreen.databinding.VnItemQuoteInputBinding
import com.futurecode.voicelockscreen.databinding.VnItemSelectQuoteBinding
import com.futurecode.voicelockscreen.utils.VoiceLockFeatureStore

/** AI Quote: reference fragment_select_quote — "Your quote" input + 2-column topic grid + View Your Quote. */
class AiQuoteFragment : BaseFragment<FragmentAiQuoteBinding>(FragmentAiQuoteBinding::inflate) {
    private data class Topic(val name: String, @StringRes val desc: Int, val image: Int)

    private val topics = listOf(
        Topic("Passion", R.string.vn_th_topic_passion_desc, R.drawable.theme_one),
        Topic("Growth", R.string.vn_th_topic_growth_desc, R.drawable.theme_two),
        Topic("Vitality", R.string.vn_th_topic_vitality_desc, R.drawable.theme_three),
        Topic("Aspire", R.string.vn_th_topic_aspire_desc, R.drawable.theme_four),
        Topic("Wisdom", R.string.vn_th_topic_wisdom_desc, R.drawable.theme_five),
        Topic("Courage", R.string.vn_th_topic_courage_desc, R.drawable.theme_six),
        Topic("Harmony", R.string.vn_th_topic_harmony_desc, R.drawable.lock_preview_wallpaper),
        Topic("Gratitude", R.string.vn_th_topic_gratitude_desc, R.drawable.theme_three),
        Topic("Dreams", R.string.vn_th_topic_dreams_desc, R.drawable.theme_two),
        Topic("Hope", R.string.vn_th_topic_hope_desc, R.drawable.theme_one),
    )
    private var topic = ""
    private var input = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        topic = savedInstanceState?.getString("topic").orEmpty()
        input = savedInstanceState?.getString("input").orEmpty()
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        val grid = GridLayoutManager(requireContext(), 2)
        grid.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (position == 0) 2 else 1
        }
        binding.rvTopics.layoutManager = grid
        binding.rvTopics.adapter = Adapter()
        val gap = resources.getDimensionPixelSize(R.dimen.vn_dp_12)
        binding.rvTopics.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(out: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                val pos = parent.getChildAdapterPosition(v)
                if (pos == RecyclerView.NO_POSITION || pos == 0) return
                out.top = gap
                val column = (pos - 1) % 2
                out.left = if (column == 0) 0 else gap / 2
                out.right = if (column == 0) gap / 2 else 0
            }
        })
        binding.btnViewQuote.setOnClickListener {
            val quote = input.trim().ifBlank { if (topic.isNotBlank()) VoiceLockFeatureStore.quoteForTopic(requireContext(), topic) else "" }
            if (quote.isBlank()) return@setOnClickListener
            VoiceLockFeatureStore.saveLockQuote(requireContext(), quote)
            findNavController().navigate(R.id.themePreviewFragment, ThemePreviewFragment.quoteArgs(quote), VoiceNoteUi.slideNavOptions)
        }
        renderButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("topic", topic); outState.putString("input", input)
    }

    /** Reference: grey pill (neutral09 / neutral07) until a quote or topic exists, then the primary gradient. */
    private fun renderButton() {
        val b = bindingOrNull ?: return
        val enabled = topic.isNotBlank() || input.isNotBlank()
        b.btnViewQuote.isEnabled = enabled
        b.btnViewQuote.setBackgroundResource(if (enabled) R.drawable.vn_th_bg_gradient_btn else R.drawable.vn_bg_radius_1000_neutral09)
        val color = ContextCompat.getColor(requireContext(), if (enabled) R.color.vn_white else R.color.vn_neutral07)
        b.tvViewYourQuote.setTextColor(color)
        b.ivStar.setColorFilter(color)
    }

    private inner class Adapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        inner class InputHolder(val b: VnItemQuoteInputBinding) : RecyclerView.ViewHolder(b.root)
        inner class TopicHolder(val b: VnItemSelectQuoteBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemViewType(position: Int) = if (position == 0) 0 else 1
        override fun getItemCount() = topics.size + 1
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            if (viewType == 0) {
                val b = VnItemQuoteInputBinding.inflate(inflater, parent, false)
                b.etQuote.setText(input)
                b.tvCounter.text = getString(R.string.vn_th_quote_counter, input.length)
                b.etQuote.doAfterTextChanged {
                    input = it?.toString().orEmpty()
                    b.tvCounter.text = getString(R.string.vn_th_quote_counter, input.length)
                    renderButton()
                }
                return InputHolder(b)
            }
            return TopicHolder(VnItemSelectQuoteBinding.inflate(inflater, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder !is TopicHolder) return
            val item = topics[position - 1]
            val selected = item.name == topic
            holder.b.tvTitle.text = item.name
            holder.b.tvDesc.setText(item.desc)
            Glide.with(holder.b.ivThumb).load(item.image).centerCrop().into(holder.b.ivThumb)
            val inset = if (selected) resources.getDimensionPixelSize(R.dimen.vn_dp_4) else 0
            holder.b.topicRoot.setPadding(inset, inset, inset, inset)
            holder.b.topicRoot.setBackgroundResource(if (selected) R.drawable.vn_th_bg_topic_selected else 0)
            holder.b.root.setOnClickListener {
                val previous = topics.indexOfFirst { it.name == topic }
                topic = if (selected) "" else item.name
                if (previous >= 0) notifyItemChanged(previous + 1)
                notifyItemChanged(holder.bindingAdapterPosition)
                renderButton()
            }
        }
    }
}
