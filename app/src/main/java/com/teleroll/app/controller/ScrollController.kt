package com.teleroll.app.controller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.Display
import android.view.WindowManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ScrollController(
    private val service: AccessibilityService,
) {
    suspend fun scrollUp(distancePercent: Int, durationMs: Long): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false

        val bounds = screenBounds()
        val centerX = bounds.centerX().toFloat()
        val bottomY = bounds.top + bounds.height() * 0.78f
        val distance = bounds.height() * (distancePercent.coerceIn(25, 88) / 100f)
        val topY = (bottomY - distance).coerceAtLeast(bounds.top + 96f)

        val path = Path().apply {
            moveTo(centerX, bottomY)
            lineTo(centerX, topY)
        }
        val stroke = GestureDescription.StrokeDescription(
            path,
            0L,
            durationMs.coerceIn(180L, 1_600L),
        )
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        return suspendCancellableCoroutine { continuation ->
            val dispatched = service.dispatchGesture(
                gesture,
                object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (continuation.isActive) continuation.resume(false)
                    }
                },
                null,
            )
            if (!dispatched && continuation.isActive) continuation.resume(false)
            continuation.invokeOnCancellation {
                service.cancelAllGestures()
            }
        }
    }

    private fun screenBounds(): Rect {
        val metrics = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        val display: Display? = service.getSystemService(WindowManager::class.java)?.defaultDisplay
        @Suppress("DEPRECATION")
        display?.getRealMetrics(metrics)
        return Rect(0, 0, metrics.widthPixels.coerceAtLeast(1), metrics.heightPixels.coerceAtLeast(1))
    }
}