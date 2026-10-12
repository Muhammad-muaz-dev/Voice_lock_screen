package com.futurecode.voicelockscreen.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.futurecode.voicelockscreen.databinding.ItemOnboardingBinding
import com.futurecode.voicelockscreen.model.OnBoardingPage

class OnBoardingAdapter(
    val list: List<OnBoardingPage>
) : RecyclerView.Adapter<OnBoardingAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val page = list[position]
        holder.binding.imgOnBoarding.setImageResource(page.imageRes)
        holder.binding.tvOnBoardingTitle.text = page.title
        holder.binding.tvOnBoardingDescription.text = page.description
    }

    override fun getItemCount(): Int = list.size
}
