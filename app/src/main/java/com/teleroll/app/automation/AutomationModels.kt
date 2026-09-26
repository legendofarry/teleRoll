package com.teleroll.app.automation

import android.graphics.Rect

enum class AutomationState {
    IDLE,
    SCROLLING,
    MEDIA_DETECTED,
    WAITING_FOR_DOWNLOAD,
    DOWNLOAD_COMPLETE,
    CONTINUING,
    PAUSED,
    FINISHED,
    STOPPED,
}

enum class DownloadState {
    DOWNLOADING,
    COMPLETE,
    UNKNOWN,
    NONE,
}

data class SettingsSnapshot(
    val scrollDistancePercent: Int = 72,
    val scrollDurationMs: Long = 620,
    val completionDelayMs: Long = 350,
    val maxScrollCount: Int = 250,
    val maxSessionDurationMinutes: Int = 30,
    val stopAtEnd: Boolean = true,
    val pauseWhenTelegramLosesFocus: Boolean = true,
    val keepScreenAwake: Boolean = true,
    val showOverlay: Boolean = true,
    val debugLogging: Boolean = false,
    val darkTheme: Boolean = true,
)

data class MediaItem(
    val fingerprint: String,
    val kind: String,
    val bounds: Rect,
    val text: String?,
    val contentDescription: String?,
    val downloadState: DownloadState,
    val progressPercent: Int?,
    val completionEvidence: String?,
)

data class TelegramUiSnapshot(
    val packageName: String?,
    val media: List<MediaItem> = emptyList(),
    val overallDownloadState: DownloadState = DownloadState.NONE,
    val highestProgressPercent: Int? = null,
    val contentFingerprint: String = "",
    val relevantNodes: List<String> = emptyList(),
    val isTelegramForeground: Boolean = false,
    val hasScrollableContent: Boolean = false,
    val endOfContentHint: Boolean = false,
) {
    val isDownloading: Boolean
        get() = media.any { it.downloadState == DownloadState.DOWNLOADING }

    val hasUnknownMedia: Boolean
        get() = media.any { it.downloadState == DownloadState.UNKNOWN }

    val allVisibleMediaComplete: Boolean
        get() = media.isNotEmpty() && media.all { it.downloadState == DownloadState.COMPLETE }
}

data class AutomationSnapshot(
    val state: AutomationState = AutomationState.IDLE,
    val statusText: String = "Ready",
    val processedMediaCount: Int = 0,
    val scrollCount: Int = 0,
    val telegramForeground: Boolean = false,
    val mediaVisibleCount: Int = 0,
    val detectedPercent: Int? = null,
    val downloadState: DownloadState = DownloadState.NONE,
    val currentPackage: String? = null,
    val relevantNodes: List<String> = emptyList(),
    val startedAtMillis: Long? = null,
    val lastContentFingerprint: String? = null,
)