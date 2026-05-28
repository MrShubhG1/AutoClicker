package com.autoclicker.master.gestures

import android.graphics.Path
import android.accessibilityservice.GestureDescription

object GestureFactory {
    
    fun createClick(x: Float, y: Float, durationMs: Long): GestureDescription {
        val clickPath = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(clickPath, 0L, durationMs.coerceAtLeast(1L))
        return GestureDescription.Builder().addStroke(stroke).build()
    }

    fun createSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long): GestureDescription {
        val swipePath = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(swipePath, 0L, durationMs.coerceAtLeast(1L))
        return GestureDescription.Builder().addStroke(stroke).build()
    }
}