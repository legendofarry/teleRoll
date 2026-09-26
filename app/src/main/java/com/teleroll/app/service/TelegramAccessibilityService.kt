package com.teleroll.app.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.teleroll.app.R
import com.teleroll.app.automation.AutomationBus
import com.teleroll.app.automation.DebugLogger
import com.teleroll.app.automation.AutomationSnapshot
import com.teleroll.app.automation.AutomationState
import com.teleroll.app.automation.AutomationStateMachine
import com.teleroll.app.automation.DownloadState
import com.teleroll.app.automation.MediaItem
import com.teleroll.app.controller.ScrollController
import com.teleroll.app.data.ServiceStateStore
import com.teleroll.app.data.SettingsRepository
import com.teleroll.app.detector.TelegramUiDetector
import com.teleroll.app.tracker.MediaSessionTracker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

class TelegramAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val uiSignals = Channel<Unit>(Channel.CONFLATED)
    private val detector = TelegramUiDetector()
    private val tracker = MediaSessionTracker()
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var scrollController: ScrollController
    private lateinit var stateMachine: AutomationStateMachine
    private lateinit var overlayController: OverlayController
    private var automationJob: Job? = null
    private val pauseRequested = AtomicBoolean(false)
    private var lastSnapshot = AutomationSnapshot()

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository = SettingsRepository(this)
        scrollController = ScrollController(this)
        stateMachine = AutomationStateMachine(::publish)
        overlayController = OverlayController(
            context = this,
            onStart = { handleAction(ACTION_START) },
            onPause = { handleAction(ACTION_PAUSE) },
            onResume = { handleAction(ACTION_RESUME) },
            onStop = { handleAction(ACTION_STOP) },
        )
        serviceInfo = serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        createNotificationChannel()
        startForegroundCompat()
        DebugLogger.enabled = settingsRepository.settings.value.debugLogging
        publish(lastSnapshot.copy(statusText = "Ready"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAutomation()
            ACTION_PAUSE -> pauseAutomation()
            ACTION_RESUME -> resumeAutomation()
            ACTION_STOP -> stopAutomation()
        }
        return START_STICKY
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() == TELEGRAM_PACKAGE ||
            event?.packageName?.toString() == TELEGRAM_WEB_PACKAGE
        ) {
            uiSignals.trySend(Unit)
        } else if (pauseRequested.get()) {
            uiSignals.trySend(Unit)
        }
    }

    override fun onInterrupt() {
        stopAutomation()
    }

    override fun onDestroy() {
        automationJob?.cancel()
        if (::overlayController.isInitialized) overlayController.hide()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startAutomation() {
        if (!::stateMachine.isInitialized) return
        pauseRequested.set(false)
        if (automationJob?.isActive == true) return
        tracker.clear()
        stateMachine.reset()
        val settings = settingsRepository.settings.value
        DebugLogger.enabled = settings.debugLogging
        if (settings.showOverlay) {
            overlayController.show()
            overlayController.setKeepScreenAwake(settings.keepScreenAwake)
        }
        automationJob = serviceScope.launch {
            runAutomation()
        }
    }

    private fun pauseAutomation() {
        pauseRequested.set(true)
        if (::stateMachine.isInitialized) stateMachine.paused("Paused by user")
    }

    private fun resumeAutomation() {
        pauseRequested.set(false)
        if (automationJob?.isActive != true) {
            automationJob = serviceScope.launch { runAutomation(resuming = true) }
        } else if (::stateMachine.isInitialized) {
            stateMachine.continuing()
        }
    }

    private fun stopAutomation() {
        pauseRequested.set(false)
        automationJob?.cancel()
        automationJob = null
        if (::stateMachine.isInitialized) stateMachine.stopped()
        overlayController.update(lastSnapshot)
    }

    private suspend fun runAutomation(resuming: Boolean = false) {
        if (!resuming) stateMachine.begin()
        val seenScreens = linkedSetOf<String>()
        var unchangedScrolls = 0
        try {
            while (serviceScope.isActive && !pauseRequested.get()) {
                val settings = settingsRepository.settings.value
                val startedAt = stateMachine.current().startedAtMillis ?: SystemClock.elapsedRealtime()
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                if (stateMachine.current().scrollCount >= settings.maxScrollCount ||
                    elapsed >= settings.maxSessionDurationMinutes * 60_000L
                ) {
                    stateMachine.finished()
                    break
                }

                val visible = detector.inspect(rootInActiveWindow)
                publishUi(visible)
                if (!visible.isTelegramForeground && settings.pauseWhenTelegramLosesFocus) {
                    stateMachine.paused("Paused — open Telegram to continue")
                    awaitUiSignal()
                    continue
                }

                val pending = visible.media.filterNot(tracker::isProcessed)
                if (pending.isNotEmpty()) {
                    stateMachine.mediaDetected(pending.size)
                    if (!awaitMediaCompletion(pending, settings.maxSessionDurationMinutes)) {
                        continue
                    }
                    tracker.markProcessed(pending)
                    stateMachine.incrementProcessed(pending.size)
                    stateMachine.downloadComplete()
                    delay(settings.completionDelayMs.coerceIn(0L, 5_000L))
                    stateMachine.continuing()
                    continue
                }

                if (visible.endOfContentHint && settings.stopAtEnd) {
                    stateMachine.finished()
                    break
                }

                val before = visible.contentFingerprint
                if (!scrollController.scrollUp(
                        settings.scrollDistancePercent,
                        settings.scrollDurationMs,
                    )
                ) {
                    stateMachine.paused("Paused — scroll gesture unavailable")
                    break
                }
                stateMachine.incrementScroll()
                awaitUiSignal(timeoutMs = settings.scrollDurationMs + 1_100L)

                val after = detector.inspect(rootInActiveWindow)
                publishUi(after)
                if (after.contentFingerprint.isNotEmpty() &&
                    (after.contentFingerprint == before || !seenScreens.add(after.contentFingerprint))
                ) {
                    unchangedScrolls += 1
                } else {
                    unchangedScrolls = 0
                    seenScreens.add(after.contentFingerprint)
                }
                if (unchangedScrolls >= 3 && settings.stopAtEnd) {
                    stateMachine.finished()
                    break
                }
            }
        } catch (_: CancellationException) {
            // Stop is an expected user action, not an error.
        } finally {
            if (stateMachine.current().state == AutomationState.FINISHED) {
                overlayController.update(lastSnapshot)
            }
        }
    }

    private suspend fun awaitMediaCompletion(
        original: List<MediaItem>,
        maxMinutes: Int,
    ): Boolean {
        while (serviceScope.isActive && !pauseRequested.get()) {
            val current = detector.inspect(rootInActiveWindow)
            publishUi(current)
            if (!current.isTelegramForeground && settingsRepository.settings.value.pauseWhenTelegramLosesFocus) {
                stateMachine.paused("Paused — open Telegram to continue")
                awaitUiSignal()
                continue
            }

            val required = current.media.filter { candidate ->
                original.any { sameVisibleMedia(candidate, it) } ||
                    (!tracker.isProcessed(candidate) && candidate.downloadState != DownloadState.NONE)
            }
            val blockers = required.filter {
                it.downloadState == DownloadState.DOWNLOADING ||
                    it.downloadState == DownloadState.UNKNOWN
            }
            val complete = required.isNotEmpty() && blockers.isEmpty()
            if (complete) return true

            val startedAt = stateMachine.current().startedAtMillis ?: SystemClock.elapsedRealtime()
            if (SystemClock.elapsedRealtime() - startedAt > maxMinutes * 60_000L) {
                stateMachine.paused("Paused — download wait exceeded session limit")
                return false
            }
            stateMachine.waiting(
                current.highestProgressPercent,
                if (blockers.any { it.downloadState == DownloadState.DOWNLOADING }) {
                    DownloadState.DOWNLOADING
                } else {
                    DownloadState.UNKNOWN
                },
            )
            // This timeout only re-reads accessibility state if Telegram does not
            // emit a content event. It is never used as proof that a download ended.
            awaitUiSignal(timeoutMs = 1_200L)
        }
        return false
    }

    private fun sameVisibleMedia(left: MediaItem, right: MediaItem): Boolean {
        if (left.kind != right.kind) return false
        val intersection = android.graphics.Rect(left.bounds)
        intersection.intersect(right.bounds)
        if (intersection.isEmpty) return false
        val overlap = intersection.width() * intersection.height()
        val smaller = minOf(
            left.bounds.width() * left.bounds.height(),
            right.bounds.width() * right.bounds.height(),
        )
        return smaller > 0 && overlap.toFloat() / smaller >= 0.35f
    }

    private suspend fun awaitUiSignal(timeoutMs: Long = 900L) {
        withTimeoutOrNull(timeoutMs) {
            uiSignals.receive()
        }
    }

    private fun publishUi(snapshot: com.teleroll.app.automation.TelegramUiSnapshot) {
        stateMachine.updateUi(
            telegramForeground = snapshot.isTelegramForeground,
            packageName = snapshot.packageName,
            mediaVisibleCount = snapshot.media.size,
            percent = snapshot.highestProgressPercent,
            downloadState = snapshot.overallDownloadState,
            relevantNodes = snapshot.relevantNodes,
            contentFingerprint = snapshot.contentFingerprint,
        )
    }

    private fun publish(snapshot: AutomationSnapshot) {
        lastSnapshot = snapshot.copy(processedMediaCount = tracker.count())
        DebugLogger.d(
            "state=${lastSnapshot.state} status=${lastSnapshot.statusText} " +
                "media=${lastSnapshot.mediaVisibleCount} percent=${lastSnapshot.detectedPercent}",
        )
        ServiceStateStore.get(this).publish(lastSnapshot)
        AutomationBus.publish(lastSnapshot)
        if (::overlayController.isInitialized) {
            val settings = settingsRepository.settings.value
            DebugLogger.enabled = settings.debugLogging
            if (settings.showOverlay) {
                overlayController.show()
                overlayController.setKeepScreenAwake(settings.keepScreenAwake)
            }
            overlayController.update(lastSnapshot)
        }
    }

    private fun handleAction(action: String) {
        ContextCompat.startForegroundService(
            this,
            Intent(this, TelegramAccessibilityService::class.java).setAction(action),
        )
    }

    private fun startForegroundCompat() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_tele_roll)
            .setContentTitle("teleRoll is ready")
            .setContentText("Waiting for a Telegram channel or group")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL,
                    "teleRoll automation",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private companion object {
        const val ACTION_START = "com.teleroll.app.action.START"
        const val ACTION_PAUSE = "com.teleroll.app.action.PAUSE"
        const val ACTION_RESUME = "com.teleroll.app.action.RESUME"
        const val ACTION_STOP = "com.teleroll.app.action.STOP"
        const val TELEGRAM_PACKAGE = "org.telegram.messenger"
        const val TELEGRAM_WEB_PACKAGE = "org.telegram.messenger.web"
        const val NOTIFICATION_CHANNEL = "tele_roll_automation"
        const val NOTIFICATION_ID = 712
    }
}