package com.airmouse.ui.onboarding

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.airmouse.R
import com.airmouse.databinding.ActivityOnboardingBinding
import com.airmouse.presentation.ui.home.HomeActivity
import com.airmouse.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: OnboardingPagerAdapter
    private var currentPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)

        // If onboarding already completed, skip directly to main activity
        if (prefs.isOnboardingCompleted()) {
            startMainActivity()
            return
        }

        setupViewPager()
        setupClickListeners()
        setupPageChangeListener()
        setupWindowInsets()
    }

    private fun setupViewPager() {
        val onboardingItems = listOf(
            OnboardingItem(
                imageRes = R.drawable.ic_air_mouse,
                title = getString(R.string.onboarding_title_1),
                description = getString(R.string.onboarding_desc_1),
                bgColor = R.color.onboarding_1_bg
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_gesture,
                title = getString(R.string.onboarding_title_2),
                description = getString(R.string.onboarding_desc_2),
                bgColor = R.color.onboarding_2_bg
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_wifi,
                title = getString(R.string.onboarding_title_3),
                description = getString(R.string.onboarding_desc_3),
                bgColor = R.color.onboarding_3_bg
            ),
            OnboardingItem(
                imageRes = R.drawable.ic_about,
                title = getString(R.string.onboarding_title_4),
                description = getString(R.string.onboarding_desc_4),
                bgColor = R.color.onboarding_4_bg
            )
        )

        adapter = OnboardingPagerAdapter(onboardingItems)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.setPageTransformer(ParallaxPageTransformer())

        // Attach TabLayout to ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()
    }

    private fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            if (currentPosition < adapter.itemCount - 1) {
                binding.viewPager.currentItem = currentPosition + 1
                animateButton(binding.btnNext)
            }
        }

        binding.btnSkip.setOnClickListener {
            animateButton(binding.btnSkip)
            finishOnboarding()
        }

        binding.btnGetStarted.setOnClickListener {
            animateButton(binding.btnGetStarted)
            finishOnboarding()
        }
    }

    private fun setupPageChangeListener() {
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentPosition = position
                updateUIForPosition(position)
                updateButtonVisibility(position)
                animatePageTransition(position)
            }
        })
    }

    private fun updateUIForPosition(position: Int) {
        val colors = intArrayOf(
            ContextCompat.getColor(this, R.color.onboarding_1_bg),
            ContextCompat.getColor(this, R.color.onboarding_2_bg),
            ContextCompat.getColor(this, R.color.onboarding_3_bg),
            ContextCompat.getColor(this, R.color.onboarding_4_bg)
        )

        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), colors[position])
        colorAnim.duration = 300
        colorAnim.addUpdateListener { animator ->
            binding.root.setBackgroundColor(animator.animatedValue as Int)
        }
        colorAnim.start()
    }

    private fun updateButtonVisibility(position: Int) {
        val isLastPage = position == adapter.itemCount - 1
        binding.btnNext.visibility = if (isLastPage) View.GONE else View.VISIBLE
        binding.btnSkip.visibility = if (isLastPage) View.GONE else View.VISIBLE
        binding.btnGetStarted.visibility = if (isLastPage) View.VISIBLE else View.GONE

        if (isLastPage) {
            animateButton(binding.btnGetStarted)
        }
    }

    private fun animatePageTransition(position: Int) {
        // Optional: animate the current page's content – handled in adapter
    }

    private fun animateButton(button: MaterialButton) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun finishOnboarding() {
        prefs.setOnboardingCompleted(true)
        startMainActivity()
    }

    private fun startMainActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onBackPressed() {
        if (currentPosition > 0) {
            binding.viewPager.currentItem = currentPosition - 1
        } else {
            finishOnboarding()
        }
    }
}

// Parallax effect for ViewPager2
class ParallaxPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.apply {
            val absPos = kotlin.math.abs(position)
            alpha = 1f - absPos.coerceIn(0f, 1f)
            scaleX = 0.95f + (1f - absPos.coerceIn(0f, 1f)) * 0.05f
            scaleY = scaleX

            // Parallax effect for the image
            val imageView = page.findViewById<android.widget.ImageView>(R.id.iv_onboarding_image)
            imageView?.translationX = -position * width * 0.3f
        }
    }
}

data class OnboardingItem(
    @androidx.annotation.DrawableRes val imageRes: Int,
    val title: String,
    val description: String,
    @androidx.annotation.ColorRes val bgColor: Int = R.color.onboarding_1_bg
)