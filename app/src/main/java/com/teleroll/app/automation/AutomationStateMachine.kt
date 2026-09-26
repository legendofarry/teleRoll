package com.teleroll.app.automation

import android.os.SystemClock

class AutomationStateMachine(
    private val publish: (AutomationSnapshot) -> Unit,
) {
    private var snapshot = AutomationSnapshot()

    fun reset() {
        snapshot = AutomationSnapshot()
        publish(snapshot)
    }

    fun begin() = transition(
        AutomationState.SCROLLING,
        "Scrolling...",
        startedAtMillis = SystemClock.elapsedRealtime(),
    )

    fun mediaDetected(count: Int) =
        transition(AutomationState.MEDIA_DETECTED, "New media detected", mediaVisibleCount = count)

    fun waiting(percent: Int?, state: DownloadState) {
        val label = when {
            percent != null && percent < 100 -> "Downloading $percent%"
            state == DownloadState.UNKNOWN -> "Waiting for download..."
            else -> "Waiting for download..."
        }
        transition(
            AutomationState.WAITING_FOR_DOWNLOAD,
            label,
            detectedPercent = percent,
            downloadState = state,
        )
    }

    fun downloadComplete() = transition(
        AutomationState.DOWNLOAD_COMPLETE,
        "Download complete",
        downloadState = DownloadState.COMPLETE,
    )

    fun continuing() = transition(
        AutomationState.CONTINUING,
        "Continuing...",
        downloadState = DownloadState.NONE,
    )

    fun paused(reason: String = "Paused — Telegram not focused") =
        transition(AutomationState.PAUSED, reason)

    fun finished() = transition(AutomationState.FINISHED, "Finished — no more content detected.")

    fun stopped() = transition(AutomationState.STOPPED, "Stopped")

    fun updateUi(
        telegramForeground: Boolean,
        packageName: String?,
        mediaVisibleCount: Int,
        percent: Int?,
        downloadState: DownloadState,
        relevantNodes: List<String>,
        contentFingerprint: String?,
    ) {
        snapshot = snapshot.copy(
            telegramForeground = telegramForeground,
            currentPackage = packageName,
            mediaVisibleCount = mediaVisibleCount,
            detectedPercent = percent,
            downloadState = downloadState,
            relevantNodes = relevantNodes,
            lastContentFingerprint = contentFingerprint,
        )
        publish(snapshot)
    }

    fun incrementScroll() {
        snapshot = snapshot.copy(scrollCount = snapshot.scrollCount + 1)
        publish(snapshot)
    }

    fun incrementProcessed(count: Int) {
        snapshot = snapshot.copy(processedMediaCount = snapshot.processedMediaCount + count)
        publish(snapshot)
    }

    fun current(): AutomationSnapshot = snapshot

    private fun transition(
        state: AutomationState,
        status: String,
        startedAtMillis: Long? = snapshot.startedAtMillis,
        mediaVisibleCount: Int = snapshot.mediaVisibleCount,
        detectedPercent: Int? = snapshot.detectedPercent,
        downloadState: DownloadState = snapshot.downloadState,
    ) {
        snapshot = snapshot.copy(
            state = state,
            statusText = status,
            startedAtMillis = startedAtMillis,
            mediaVisibleCount = mediaVisibleCount,
            detectedPercent = detectedPercent,
            downloadState = downloadState,
        )
        publish(snapshot)
    }
}