package com.airmouse.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.airmouse.databinding.ItemOnboardingBinding

class OnboardingPagerAdapter(
    private val items: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class OnboardingViewHolder(
        private val binding: ItemOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnboardingItem) {
            binding.onboardingImage.setImageResource(item.imageRes)
            binding.onboardingTitle.text = item.title
            binding.onboardingDescription.text = item.description

            
            binding.onboardingImage.alpha = 0f
            binding.onboardingImage.translationY = 50f
            binding.onboardingImage.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()

            binding.onboardingTitle.alpha = 0f
            binding.onboardingTitle.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(200)
                .start()

            binding.onboardingDescription.alpha = 0f
            binding.onboardingDescription.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(400)
                .start()
        }
    }
}