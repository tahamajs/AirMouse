package com.airmouse

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xmlpull.v1.XmlPullParser

class NavigationGraphSanityTest {

    @Test
    fun navGraphIncludesCoreUserJourneys() {
        val parser = ApplicationProvider.getApplicationContext<android.content.Context>()
            .resources
            .getXml(R.navigation.nav_graph)

        var startDestination: String? = null
        val destinations = mutableSetOf<String>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            when (parser.name) {
                "navigation" -> startDestination = parser.getAttributeValue(
                    "http://schemas.android.com/apk/res/android", "startDestination"
                )
                "fragment", "activity" -> {
                    parser.getAttributeValue(
                        "http://schemas.android.com/apk/res/android", "name"
                    )?.let(destinations::add)
                }
            }
        }

        assertEquals("@id/homeFragment", startDestination)
        assertTrue(destinations.contains("com.airmouse.HomeFragment"))
        assertTrue(destinations.contains("com.airmouse.SettingsFragment"))
        assertTrue(destinations.contains("com.airmouse.HelpFragment"))
        assertTrue(destinations.contains("com.airmouse.ui.onboarding.OnboardingActivity"))
    }
}
