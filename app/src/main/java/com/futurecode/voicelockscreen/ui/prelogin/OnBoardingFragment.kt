package com.futurecode.voicelockscreen.ui.prelogin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.futurecode.voicelockscreen.R
import com.futurecode.voicelockscreen.activity.HomeActivity
import com.futurecode.voicelockscreen.activity.MyApplication
import com.futurecode.voicelockscreen.base.BaseFragment
import com.futurecode.voicelockscreen.databinding.FragmentOnBoardingBinding
import com.futurecode.voicelockscreen.databinding.ItemOnboardingBinding

class OnBoardingFragment : BaseFragment<FragmentOnBoardingBinding>(FragmentOnBoardingBinding::inflate) {

    private val pages = listOf(
        OnBoardingPage(
            imageRes = R.drawable.onboard_one,
            title = "Command With\nYour Voice",
            description = "Unlock your phone using\nadvanced AI Voice recognition"
        ),
        OnBoardingPage(
            imageRes = R.drawable.onboard_two,
            title = "Smart Voice\nRecognition",
            description = "Your unique voice keeps\nyour phone secure"
        ),
        OnBoardingPage(
            imageRes = R.drawable.onboard_three,
            title = "All-In-One\nProtection",
            description = "Multiple Lock Options for Stronger\nEveryday Phone Security"
        )
    )

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateIndicator(position)
            binding.btnNext.text = getString(
                if (position == pages.lastIndex) R.string.get_started else R.string.next
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.onBoardingPager.adapter = OnBoardingAdapter(pages)
        binding.onBoardingPager.registerOnPageChangeCallback(pageChangeCallback)
        setupIndicator()
        updateIndicator(0)

        binding.btnNext.setOnClickListener {
            val currentPage = binding.onBoardingPager.currentItem
            if (currentPage < pages.lastIndex) {
                binding.onBoardingPager.currentItem = currentPage + 1
            } else {
                finishOnBoarding()
            }
        }
    }

    override fun onDestroyView() {
        bindingOrNull?.onBoardingPager?.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }

    private fun setupIndicator() {
        binding.pageIndicator.removeAllViews()
        pages.forEach { _ ->
            val indicator = View(requireContext())
            binding.pageIndicator.addView(indicator)
        }
    }

    private fun updateIndicator(selectedPosition: Int) {
        for (index in 0 until binding.pageIndicator.childCount) {
            val indicator = binding.pageIndicator.getChildAt(index)
            val isSelected = index == selectedPosition
            val width = if (isSelected) 42.dp else 14.dp
            val height = 14.dp
            val margin = 5.dp

            indicator.background = ContextCompat.getDrawable(
                requireContext(),
                if (isSelected) R.drawable.bg_indicator_active else R.drawable.bg_indicator_inactive
            )
            indicator.layoutParams = LinearLayout.LayoutParams(width, height).apply {
                marginStart = margin
                marginEnd = margin
            }
        }
    }

    private fun finishOnBoarding() {
        MyApplication.app.prefManager.isOnboardingDone = true
        val intent = Intent(requireContext(), HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private data class OnBoardingPage(
        val imageRes: Int,
        val title: String,
        val description: String
    )

    private class OnBoardingAdapter(
        private val pages: List<OnBoardingPage>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<OnBoardingAdapter.OnBoardingViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardingViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemOnboardingBinding.inflate(inflater, parent, false)
            return OnBoardingViewHolder(binding)
        }

        override fun onBindViewHolder(holder: OnBoardingViewHolder, position: Int) {
            holder.bind(pages[position])
        }

        override fun getItemCount(): Int = pages.size

        private class OnBoardingViewHolder(
            private val binding: ItemOnboardingBinding
        ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(page: OnBoardingPage) {
                binding.imgOnBoarding.setImageResource(page.imageRes)
                binding.imgOnBoarding.scaleType = ImageView.ScaleType.FIT_CENTER
                binding.tvOnBoardingTitle.text = page.title
                binding.tvOnBoardingDescription.text = page.description
            }
        }
    }
}
