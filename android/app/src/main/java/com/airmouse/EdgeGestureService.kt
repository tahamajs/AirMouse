package com.airmouse

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class EdgeGestureService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
