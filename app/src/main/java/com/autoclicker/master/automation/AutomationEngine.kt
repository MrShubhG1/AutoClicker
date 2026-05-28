package com.autoclicker.master.automation

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.accessibilityservice.GestureDescription
import com.autoclicker.master.accessibility.AutoAccessibilityService
import com.autoclicker.master.gestures.GestureFactory
import com.autoclicker.master.storage.ClickPoint

class AutomationEngine private constructor() {
    companion object {
        val instance = AutomationEngine()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var currentPoints = listOf<ClickPoint>()
    private var currentIndex = 0

    fun startAutomation(points: List<ClickPoint>) {
        if (isRunning || points.isEmpty()) return
        currentPoints = points
        currentIndex = 0
        isRunning = true
        executeNextStep()
    }

    fun stopAutomation() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun executeNextStep() {
        if (!isRunning) return

        val service = AutoAccessibilityService.getInstance()
        if (service == null) {
            stopAutomation()
            return
        }

        val point = currentPoints[currentIndex]
        val gesture = GestureFactory.createClick(point.x.toFloat(), point.y.toFloat(), point.durationMs)

        service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                scheduleNext(point.delayMs)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                scheduleNext(point.delayMs)
            }
        }, null)
    }

    private fun scheduleNext(delay: Long) {
        handler.postDelayed({
            if (isRunning) {
                currentIndex = (currentIndex + 1) % currentPoints.size
                executeNextStep()
            }
        }, delay.coerceAtLeast(5L))
    }
}