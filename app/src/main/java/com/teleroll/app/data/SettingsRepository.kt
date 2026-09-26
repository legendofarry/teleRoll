package com.teleroll.app.data

import android.content.Context
import com.teleroll.app.automation.SettingsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("teleroll_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<SettingsSnapshot> = _settings.asStateFlow()

    fun update(transform: (SettingsSnapshot) -> SettingsSnapshot) {
        val next = transform(_settings.value)
        preferences.edit()
            .putInt(KEY_DISTANCE, next.scrollDistancePercent)
            .putLong(KEY_SPEED, next.scrollDurationMs)
            .putLong(KEY_DELAY, next.completionDelayMs)
            .putInt(KEY_MAX_SCROLLS, next.maxScrollCount)
            .putInt(KEY_MAX_DURATION, next.maxSessionDurationMinutes)
            .putBoolean(KEY_STOP_END, next.stopAtEnd)
            .putBoolean(KEY_PAUSE_FOCUS, next.pauseWhenTelegramLosesFocus)
            .putBoolean(KEY_KEEP_AWAKE, next.keepScreenAwake)
            .putBoolean(KEY_OVERLAY, next.showOverlay)
            .putBoolean(KEY_DEBUG, next.debugLogging)
            .putBoolean(KEY_DARK_THEME, next.darkTheme)
            .apply()
        _settings.value = next
    }

    private fun load(): SettingsSnapshot = SettingsSnapshot(
        scrollDistancePercent = preferences.getInt(KEY_DISTANCE, 72),
        scrollDurationMs = preferences.getLong(KEY_SPEED, 620),
        completionDelayMs = preferences.getLong(KEY_DELAY, 350),
        maxScrollCount = preferences.getInt(KEY_MAX_SCROLLS, 250),
        maxSessionDurationMinutes = preferences.getInt(KEY_MAX_DURATION, 30),
        stopAtEnd = preferences.getBoolean(KEY_STOP_END, true),
        pauseWhenTelegramLosesFocus = preferences.getBoolean(KEY_PAUSE_FOCUS, true),
        keepScreenAwake = preferences.getBoolean(KEY_KEEP_AWAKE, true),
        showOverlay = preferences.getBoolean(KEY_OVERLAY, true),
        debugLogging = preferences.getBoolean(KEY_DEBUG, false),
        darkTheme = preferences.getBoolean(KEY_DARK_THEME, true),
    )

    private companion object {
        const val KEY_DISTANCE = "scroll_distance"
        const val KEY_SPEED = "scroll_duration"
        const val KEY_DELAY = "completion_delay"
        const val KEY_MAX_SCROLLS = "max_scrolls"
        const val KEY_MAX_DURATION = "max_duration"
        const val KEY_STOP_END = "stop_at_end"
        const val KEY_PAUSE_FOCUS = "pause_focus"
        const val KEY_KEEP_AWAKE = "keep_awake"
        const val KEY_OVERLAY = "overlay"
        const val KEY_DEBUG = "debug_logging"
        const val KEY_DARK_THEME = "dark_theme"
    }
}