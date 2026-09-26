package com.teleroll.app.detector

import com.teleroll.app.automation.DownloadState
import java.util.Locale
import java.util.regex.Pattern

data class DownloadSignal(
    val state: DownloadState,
    val percent: Int?,
    val evidence: String?,
)

/**
 * Keeps progress interpretation separate from media-surface heuristics.
 * Unknown is deliberately a first-class result.
 */
class DownloadProgressDetector {
    fun inspect(
        text: String?,
        className: String,
        hasCompletionEvidence: Boolean,
    ): DownloadSignal {
        val source = text.orEmpty().lowercase(Locale.US)
        val percent = PERCENT.matcher(source).run {
            if (find()) group(1)?.toIntOrNull()?.coerceIn(0, 100) else null
        }
        return when {
            percent != null && percent >= 100 ->
                DownloadSignal(DownloadState.COMPLETE, percent, "100% text")
            percent != null ->
                DownloadSignal(DownloadState.DOWNLOADING, percent, "percentage text")
            className.contains("Progress", ignoreCase = true) ||
                source.contains("downloading") ||
                source.contains("loading") ->
                DownloadSignal(DownloadState.DOWNLOADING, null, "loading semantics")
            hasCompletionEvidence ->
                DownloadSignal(DownloadState.COMPLETE, null, "visible loaded surface")
            else ->
                DownloadSignal(DownloadState.UNKNOWN, null, null)
        }
    }

    private companion object {
        val PERCENT = Pattern.compile("""(?<!\d)(\d{1,3})\s*%""")
    }
}