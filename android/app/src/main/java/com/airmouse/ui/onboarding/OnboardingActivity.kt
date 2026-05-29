package com.airmouse.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewpager2.widget.ViewPager2
import com.airmouse.R
import com.airmouse.ui.MainActivity
import com.airmouse.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        prefs = PreferencesManager(this)
        if (prefs.isOnboardingCompleted()) {
            startMainActivity()
            return
        }

        setContentView(R.layout.activity_onboarding)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val btnSkip = findViewById<MaterialButton>(R.id.btn_skip)
        val btnNext = findViewById<MaterialButton>(R.id.btn_next)
        val btnGetStarted = findViewById<MaterialButton>(R.id.btn_get_started)

        val onboardingItems = listOf(
            OnboardingItem(
                R.drawable.ic_air_mouse,
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
                R.drawable.ic_about,
                getString(R.string.onboarding_title_4),
                getString(R.string.onboarding_desc_4)
            )
        )

        viewPager.adapter = OnboardingAdapter(onboardingItems)

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val isLastPage = position == onboardingItems.size - 1
                btnNext.visibility = if (isLastPage) View.GONE else View.VISIBLE
                btnSkip.visibility = if (isLastPage) View.GONE else View.VISIBLE
                btnGetStarted.visibility = if (isLastPage) View.VISIBLE else View.GONE
            }
        })

        btnNext.setOnClickListener {
            viewPager.currentItem += 1
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }

        btnGetStarted.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun finishOnboarding() {
        prefs.setOnboardingCompleted(true)
        startMainActivity()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}