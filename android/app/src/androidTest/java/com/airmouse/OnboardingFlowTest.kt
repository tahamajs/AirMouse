package com.airmouse

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.airmouse.ui.onboarding.OnboardingActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.assertTrue

@RunWith(AndroidJUnit4::class)
class OnboardingFlowTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs by lazy { PreferencesManager(context) }

    @Before
    fun resetOnboardingFlag() {
        prefs.setOnboardingCompleted(false)
    }

    @Test
    fun onboardingShowsFirstPageWithClearActions() {
        ActivityScenario.launch(OnboardingActivity::class.java).use {
            onView(withId(R.id.view_pager)).check(matches(isDisplayed()))
            onView(withId(R.id.btn_skip)).check(matches(isDisplayed()))
            onView(withId(R.id.btn_next)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun swipingToLastPageShowsGetStartedButton() {
        ActivityScenario.launch(OnboardingActivity::class.java).use {
            onView(withId(R.id.view_pager)).perform(swipeLeft())
            onView(withId(R.id.view_pager)).perform(swipeLeft())
            onView(withId(R.id.view_pager)).perform(swipeLeft())
            onView(withId(R.id.btn_get_started)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun skipMarksOnboardingComplete() {
        ActivityScenario.launch(OnboardingActivity::class.java).use {
            onView(withId(R.id.btn_skip)).perform(click())
        }
        assertTrue(prefs.isOnboardingCompleted())
    }

    @Test
    fun completedOnboardingSkipsToMainFlow() {
        prefs.setOnboardingCompleted(true)
        ActivityScenario.launch(OnboardingActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }
}
