package com.airmouse.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.airmouse.R
import com.airmouse.MainActivity
import com.airmouse.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnSkip: MaterialButton
    private lateinit var btnNext: MaterialButton
    private lateinit var btnGetStarted: MaterialButton
    private lateinit var preferences: PreferencesManager

    private lateinit var onboardingItems: List<OnboardingItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        preferences = PreferencesManager(this)
        onboardingItems = createOnboardingItems()

        viewPager = findViewById(R.id.view_pager)
        tabLayout = findViewById(R.id.tab_layout)
        btnSkip = findViewById(R.id.btn_skip)
        btnNext = findViewById(R.id.btn_next)
        btnGetStarted = findViewById(R.id.btn_get_started)

        val adapter = OnboardingAdapter(onboardingItems)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateButtons(position)
            }
        })

        btnSkip.setOnClickListener {
            finishOnboarding()
        }

        btnNext.setOnClickListener {
            val nextItem = viewPager.currentItem + 1
            if (nextItem < onboardingItems.size) {
                viewPager.currentItem = nextItem
            } else {
                finishOnboarding()
            }
        }

        btnGetStarted.setOnClickListener {
            finishOnboarding()
        }

        updateButtons(0)
    }

    private fun updateButtons(position: Int) {
        val isLast = position == onboardingItems.size - 1
        btnNext.visibility = if (isLast) android.view.View.GONE else android.view.View.VISIBLE
        btnGetStarted.visibility = if (isLast) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun finishOnboarding() {
        preferences.setOnboardingCompleted(true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun createOnboardingItems(): List<OnboardingItem> {
        return listOf(
            OnboardingItem(
                R.drawable.ic_air_mouse_pro_ultra,
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1)
            ),
            OnboardingItem(
                R.drawable.ic_gesture,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2)
            ),
            OnboardingItem(
                R.drawable.ic_wifi,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3)
            ),
            OnboardingItem(
                R.drawable.ic_stats,
                getString(R.string.onboarding_title_4),
                getString(R.string.onboarding_desc_4)
            )
        )
    }
}
