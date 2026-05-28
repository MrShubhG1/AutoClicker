package com.autoclicker.master.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference

class AutoAccessibilityService : AccessibilityService() {

    companion object {
        private var instanceRef: WeakReference<AutoAccessibilityService>? = null
        
        fun getInstance(): AutoAccessibilityService? {
            return instanceRef?.get()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRef = WeakReference(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instanceRef = null
    }
}