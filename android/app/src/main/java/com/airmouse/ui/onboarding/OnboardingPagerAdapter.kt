package com.airmouse.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.airmouse.databinding.ItemOnboardingBinding

/**
 * Pager adapter for the onboarding screens.
 * Displays a list of OnboardingItem with smooth enter animations.
 */
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
        // Animate only the first page (or all pages if desired)
        val animate = position == 0
        holder.bind(items[position], animate)
    }

    override fun getItemCount(): Int = items.size

    override fun onViewRecycled(holder: OnboardingViewHolder) {
        super.onViewRecycled(holder)
        // Ensure all animations are cancelled and views are reset
        holder.clearAnimations()
    }

    /**
     * ViewHolder for a single onboarding page.
     */
    class OnboardingViewHolder(
        private val binding: ItemOnboardingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Bind the data to the views and optionally animate them in.
         *
         * @param item The OnboardingItem to display.
         * @param animate Whether to play the entrance animation.
         */
        fun bind(item: OnboardingItem, animate: Boolean = true) {
            // Set data
            binding.onboardingImage.setImageResource(item.imageRes)
            binding.onboardingTitle.text = item.title
            binding.onboardingDescription.text = item.description

            // Accessibility
            binding.onboardingImage.contentDescription = item.title
            binding.onboardingTitle.contentDescription = item.title

            if (animate) {
                // Reset to initial state for animation
                binding.onboardingImage.alpha = 0f
                binding.onboardingImage.translationY = 50f
                binding.onboardingTitle.alpha = 0f
                binding.onboardingDescription.alpha = 0f

                // Animate image (fade + slide up)
                binding.onboardingImage.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                // Animate title (fade in with slight delay)
                binding.onboardingTitle.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(150)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                // Animate description (fade in with longer delay)
                binding.onboardingDescription.animate()
                    .alpha(1f)
                    .setDuration(500)
                    .setStartDelay(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            } else {
                // No animation – set fully visible immediately
                binding.onboardingImage.alpha = 1f
                binding.onboardingImage.translationY = 0f
                binding.onboardingTitle.alpha = 1f
                binding.onboardingDescription.alpha = 1f
            }
        }

        /**
         * Cancel any pending animations and reset views to default state.
         * Called when the ViewHolder is recycled.
         */
        fun clearAnimations() {
            binding.onboardingImage.animate().cancel()
            binding.onboardingTitle.animate().cancel()
            binding.onboardingDescription.animate().cancel()

            // Reset to fully visible (avoids stuck states)
            binding.onboardingImage.alpha = 1f
            binding.onboardingImage.translationY = 0f
            binding.onboardingTitle.alpha = 1f
            binding.onboardingDescription.alpha = 1f
        }
    }
}