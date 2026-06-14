package com.airmouse.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airmouse.R

class OnboardingPagerAdapter(
    private val items: List<OnboardingItem>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.iv_onboarding_image)
        private val titleView: TextView = itemView.findViewById(R.id.tv_onboarding_title)
        private val descriptionView: TextView = itemView.findViewById(R.id.tv_onboarding_description)

        fun bind(item: OnboardingItem) {
            imageView.setImageResource(item.imageRes)
            titleView.text = item.title
            descriptionView.text = item.description

            // Simple fade‑in animation
            imageView.alpha = 0f
            imageView.translationY = 50f
            imageView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .start()

            titleView.alpha = 0f
            titleView.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(200)
                .start()

            descriptionView.alpha = 0f
            descriptionView.animate()
                .alpha(1f)
                .setDuration(500)
                .setStartDelay(400)
                .start()
        }
    }
}