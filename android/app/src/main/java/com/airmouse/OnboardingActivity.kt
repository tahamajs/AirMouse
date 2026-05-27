package com.airmouse
import android.view.View

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
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

    private val onboardingItems by lazy {
        listOf(
            OnboardingItem(
                R.drawable.ic_calibrate, // Placeholder if ic_air_mouse_pro_ultra missing
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1)
            ),
            OnboardingItem(
                R.drawable.ic_calibrate,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2)
            ),
            OnboardingItem(
                R.drawable.ic_calibrate,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3)
            ),
            OnboardingItem(
                R.drawable.ic_calibrate,
                getString(R.string.onboarding_title_4),
                getString(R.string.onboarding_desc_4)
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        preferences = PreferencesManager(this)

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
        btnNext.visibility = if (isLast) View.GONE else View.VISIBLE
        btnGetStarted.visibility = if (isLast) View.VISIBLE else View.GONE
    }

    private fun finishOnboarding() {
        preferences.setOnboardingCompleted(true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}