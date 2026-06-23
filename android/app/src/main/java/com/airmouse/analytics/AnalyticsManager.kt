
package com.airmouse.analytics

import android.content.Context
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var sessionId: String = ""
    private var sessionStartTime: Long = 0

    data class AnalyticsEvent(
        val name: String,
        val properties: Map<String, Any> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )

    fun trackEvent(event: AnalyticsEvent) {
        if (!prefs.getBoolean("analytics_enabled", true)) return

        scope.launch {
            
            android.util.Log.d("Analytics", "Event: ${event.name}, Properties: ${event.properties}")

            
            storeEventLocally(event)
        }
    }

    fun trackScreenView(screenName: String) {
        trackEvent(AnalyticsEvent("screen_view", mapOf("screen" to screenName)))
    }

    fun trackAction(action: String, category: String = "user_action") {
        trackEvent(AnalyticsEvent("action", mapOf(
            "action" to action,
            "category" to category
        )))
    }

    fun trackGesture(gesture: String, confidence: Float) {
        trackEvent(AnalyticsEvent("gesture", mapOf(
            "gesture" to gesture,
            "confidence" to confidence
        )))
    }

    fun trackError(error: String, type: String) {
        trackEvent(AnalyticsEvent("error", mapOf(
            "error" to error,
            "type" to type
        )))
    }

    private fun storeEventLocally(event: AnalyticsEvent) {
        val storedEvents = prefs.getString("analytics_events", "")
        val newEvent = "${event.timestamp}|${event.name}|${event.properties}"
        val updatedEvents = if (storedEvents.isEmpty()) newEvent else "$storedEvents,$newEvent"
        prefs.putString("analytics_events", updatedEvents)
    }

    fun startSession() {
        sessionId = java.util.UUID.randomUUID().toString()
        sessionStartTime = System.currentTimeMillis()
        trackEvent(AnalyticsEvent("session_start", mapOf("session_id" to sessionId)))
    }

    fun endSession() {
        val duration = System.currentTimeMillis() - sessionStartTime
        trackEvent(AnalyticsEvent("session_end", mapOf(
            "session_id" to sessionId,
            "duration" to duration
        )))
    }
}