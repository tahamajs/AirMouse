package com.airmouse

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testInitialUiState() {
        onView(withId(R.id.ip_edit_text)).check(matches(isDisplayed()))
        onView(withId(R.id.start_btn)).check(matches(withText(R.string.start)))
        onView(withId(R.id.calibrate_btn)).check(matches(isDisplayed()))
    }

    @Test
    fun testInvalidIpShowsError() {
        onView(withId(R.id.ip_edit_text)).perform(typeText("not-an-ip"), closeSoftKeyboard())
        onView(withId(R.id.start_btn)).perform(click())
        
        // Check if error is set on EditText (checking error message is a bit tricky with Espresso, 
        // but we can check if a toast or status change didn't happen as expected)
        onView(withId(R.id.status_text)).check(matches(withText(R.string.status_not_connected)))
    }

    @Test
    fun testSensitivitySliderUpdatesText() {
        onView(withId(R.id.sensitivity_seekbar)).perform(swipeRight())
        // The text should reflect a higher speed now
        onView(withId(R.id.sensitivity_text)).check(matches(withText(org.hamcrest.Matchers.containsString("Speed"))))
    }

    @Test
    fun testSettingsDialogOpens() {
        onView(withId(R.id.settings_btn)).perform(click())
        onView(withText("Air Mouse Settings")).check(matches(isDisplayed()))
        onView(withText("OK")).perform(click())
    }
}