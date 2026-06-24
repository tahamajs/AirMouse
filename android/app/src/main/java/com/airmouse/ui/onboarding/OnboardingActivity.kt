package com.airmouse.ui.onboarding

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
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

    // Color palette for each page (matching the background colors)
    private val pageColors by lazy {
        intArrayOf(
            ContextCompat.getColor(this, R.color.onboarding_1_bg),
            ContextCompat.getColor(this, R.color.onboarding_2_bg),
            ContextCompat.getColor(this, R.color.onboarding_3_bg),
            ContextCompat.getColor(this, R.color.onboarding_4_bg)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Fallback if inflation fails
        try {
            binding = ActivityOnboardingBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            e.printStackTrace()
            startMainActivity()
            return
        }

        prefs = PreferencesManager(applicationContext)

        // Skip if already completed
        if (prefs.isOnboardingCompleted()) {
            startMainActivity()
            return
        }

        setupWindow()
        setupViewPager()
        setupClickListeners()
        setupPageChangeListener()
        setupWindowInsets()
        setupBackPressedHandler()
    }

    private fun setupWindow() {
        // Make it truly immersive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
        // Set initial background (will animate later)
        binding.root.setBackgroundColor(pageColors[0])
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

        // Advanced page transformer with parallax and depth
        binding.viewPager.setPageTransformer { page, position ->
            // Scale effect (zoom out slightly)
            val scale = 0.9f + (1f - kotlin.math.abs(position).coerceIn(0f, 1f)) * 0.1f
            page.scaleX = scale
            page.scaleY = scale

            // Parallax: move the page horizontally
            val translationXFactor = -position * page.width * 0.15f
            page.translationX = translationXFactor

            // Fade effect
            page.alpha = 1f - kotlin.math.abs(position).coerceIn(0f, 1f) * 0.3f

            // Slight rotation for a 3D feel
            page.rotationY = position * 8f
        }

        // Dot indicator
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { _, _ -> }.attach()

        // Style dots – use a fallback color if `gray_light` is missing
        val dotColor = ContextCompat.getColor(this, android.R.color.darker_gray)
        val selectedDotColor = ContextCompat.getColor(this, android.R.color.white)
        binding.tabLayout.setSelectedTabIndicatorColor(selectedDotColor)
        binding.tabLayout.setTabTextColors(dotColor, selectedDotColor)
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
                animateBackgroundColor(position)
            }
        })
    }

    private fun updateUIForPosition(position: Int) {
        // Use hardcoded strings instead of resources to avoid missing resource errors
        when (position) {
            0 -> binding.btnNext.text = "Next"
            1 -> binding.btnNext.text = "Next"
            2 -> binding.btnNext.text = "Next"
            3 -> binding.btnNext.text = "Get Started"
        }
    }

    private fun animateBackgroundColor(position: Int) {
        val fromColor = pageColors.getOrNull(position) ?: pageColors[0]
        val toColor = pageColors.getOrNull(position) ?: pageColors[0]
        val colorAnim = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor)
        colorAnim.duration = 400
        colorAnim.interpolator = DecelerateInterpolator()
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
    }

    private fun animateButton(button: MaterialButton) {
        button.animate()
            .scaleX(0.92f)
            .scaleY(0.92f)
            .setDuration(120)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                button.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .setInterpolator(DecelerateInterpolator())
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

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentPosition > 0) {
                    binding.viewPager.currentItem = currentPosition - 1
                } else {
                    finishOnboarding()
                }
            }
        })
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

    // Clean up listeners to avoid leaks
    override fun onDestroy() {
        super.onDestroy()
        binding.viewPager.adapter = null
    }
}

/**
 * Extension to get the solid background color from a View (for fallback)
 */
private val View.solidBackgroundColorOrNull: Int?
    get() = (background as? ColorDrawable)?.color