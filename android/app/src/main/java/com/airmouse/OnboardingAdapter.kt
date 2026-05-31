package com.airmouse.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airmouse.R

data class OnboardingItem(
    val imageRes: Int,
    val title: String,
    val description: String,
    val showStepIndicator: Boolean = false,
    val stepNumber: Int = 0,
    val totalSteps: Int = 0,
    val showHint: Boolean = false,
    val showNewBadge: Boolean = false
)

class OnboardingAdapter(private val items: List<OnboardingItem>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageResource(item.imageRes)
        holder.titleView.text = item.title
        holder.descriptionView.text = item.description

        // Step indicator
        if (item.showStepIndicator && item.totalSteps > 0) {
            holder.stepIndicator.text = "${item.stepNumber} / ${item.totalSteps}"
            holder.stepIndicator.visibility = View.VISIBLE
        } else {
            holder.stepIndicator.visibility = View.GONE
        }

        // Hint text
        if (item.showHint) {
            holder.hintText.visibility = View.VISIBLE
        } else {
            holder.hintText.visibility = View.GONE
        }

        // New badge
        if (item.showNewBadge) {
            holder.badgeNew.visibility = View.VISIBLE
        } else {
            holder.badgeNew.visibility = View.GONE
        }

        // Progress dots (optional, you can implement logic here)
    }

    override fun getItemCount(): Int = items.size

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.onboarding_image)
        val titleView: TextView = itemView.findViewById(R.id.onboarding_title)
        val descriptionView: TextView = itemView.findViewById(R.id.onboarding_description)
        val stepIndicator: TextView = itemView.findViewById(R.id.step_indicator)
        val hintText: TextView = itemView.findViewById(R.id.hint_text)
        val badgeNew: View = itemView.findViewById(R.id.badge_new)
    }
}