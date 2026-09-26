package com.teleroll.app.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.teleroll.app.automation.AutomationSnapshot
import com.teleroll.app.automation.AutomationState

class OverlayController(
    private val context: Context,
    private val onStart: () -> Unit,
    private val onPause: () -> Unit,
    private val onResume: () -> Unit,
    private val onStop: () -> Unit,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var root: LinearLayout? = null
    private var status: TextView? = null
    private var count: TextView? = null
    private var windowParams: WindowManager.LayoutParams? = null

    fun show() {
        if (root != null || !Settings.canDrawOverlays(context)) return

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(9))
            background = roundedBackground(ColorUtils.setAlphaComponent(Color.rgb(20, 20, 37), 242), 18)
            elevation = dp(12).toFloat()
        }

        val handle = TextView(context).apply {
            text = "teleRoll  ·  LIVE"
            setTextColor(Color.rgb(197, 187, 255))
            textSize = 11f
            letterSpacing = 0.12f
            setPadding(0, 0, 0, dp(5))
        }
        card.addView(handle, LinearLayout.LayoutParams(-1, dp(24)))

        status = TextView(context).apply {
            text = "Ready"
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 1
        }
        card.addView(status, LinearLayout.LayoutParams(-1, dp(25)))

        count = TextView(context).apply {
            text = "0 media  ·  0 scrolls"
            setTextColor(Color.rgb(163, 163, 185))
            textSize = 11f
        }
        card.addView(count, LinearLayout.LayoutParams(-1, dp(22)))

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        buttons.addView(action("START", onStart), buttonParams())
        buttons.addView(action("PAUSE", onPause), buttonParams())
        buttons.addView(action("RESUME", onResume), buttonParams())
        buttons.addView(action("STOP", onStop, accent = true), buttonParams())
        card.addView(buttons, LinearLayout.LayoutParams(-1, dp(36)))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(96)
        }

        val dragListener = dragListener(params)
        handle.setOnTouchListener(dragListener)
        card.setOnTouchListener(dragListener)

        root = card
        windowParams = params
        windowManager?.addView(card, params)
    }

    fun hide() {
        root?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        root = null
        status = null
        count = null
        windowParams = null
    }

    fun update(snapshot: AutomationSnapshot) {
        status?.text = snapshot.statusText
        count?.text = "${snapshot.processedMediaCount} media  ·  ${snapshot.scrollCount} scrolls"
    }

    fun setKeepScreenAwake(enabled: Boolean) {
        val params = windowParams ?: return
        val nextFlags = if (enabled) {
            params.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        } else {
            params.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON.inv()
        }
        if (nextFlags == params.flags) return
        params.flags = nextFlags
        root?.let { windowManager?.updateViewLayout(it, params) }
    }

    private fun action(label: String, click: () -> Unit, accent: Boolean = false) =
        TextView(context).apply {
            text = label
            textSize = 9f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(if (accent) Color.rgb(255, 198, 207) else Color.WHITE)
            background = roundedBackground(
                if (accent) ColorUtils.setAlphaComponent(Color.rgb(239, 104, 132), 72)
                else ColorUtils.setAlphaComponent(Color.rgb(255, 255, 255), 20),
                10,
            )
            setOnClickListener { click() }
            isClickable = true
        }

    private fun buttonParams() = LinearLayout.LayoutParams(0, dp(30), 1f).apply {
        marginEnd = dp(4)
    }

    private fun dragListener(params: WindowManager.LayoutParams) =
        View.OnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                view.setTag(TAG_DOWN_X, event.rawX)
                view.setTag(TAG_DOWN_Y, event.rawY)
                view.setTag(TAG_ORIGINAL_X, params.x)
                view.setTag(TAG_ORIGINAL_Y, params.y)
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                val downX = view.getTag(TAG_DOWN_X) as? Float ?: event.rawX
                val downY = view.getTag(TAG_DOWN_Y) as? Float ?: event.rawY
                val originalX = view.getTag(TAG_ORIGINAL_X) as? Int ?: params.x
                val originalY = view.getTag(TAG_ORIGINAL_Y) as? Int ?: params.y
                params.x = originalX - (event.rawX - downX).toInt()
                params.y = originalY + (event.rawY - downY).toInt()
                root?.let { windowManager?.updateViewLayout(it, params) }
                true
            } else {
                false
            }
        }

    private fun roundedBackground(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), ColorUtils.setAlphaComponent(Color.WHITE, 25))
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private companion object {
        const val TAG_DOWN_X = 1001
        const val TAG_DOWN_Y = 1002
        const val TAG_ORIGINAL_X = 1003
        const val TAG_ORIGINAL_Y = 1004
    }
}