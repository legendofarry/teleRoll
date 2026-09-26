package com.teleroll.app.detector

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.teleroll.app.automation.DownloadState
import com.teleroll.app.automation.MediaItem
import com.teleroll.app.automation.TelegramUiSnapshot
import java.security.MessageDigest
import java.util.Locale
import java.util.regex.Pattern

/**
 * Heuristic Telegram UI reader.
 *
 * Telegram does not expose a stable public accessibility contract for every media
 * surface. This detector intentionally combines text, content descriptions, view
 * ids,
 * class names, bounds, and sibling signals. An ambiguous candidate is UNKNOWN,
 * which is treated as unsafe to scroll past by the automation loop.
 */
class TelegramUiDetector {
    private val downloadProgressDetector = DownloadProgressDetector()

    fun inspect(root: AccessibilityNodeInfo?): TelegramUiSnapshot {
        if (root == null) {
            return TelegramUiSnapshot(packageName = null)
        }

        val nodes = mutableListOf<NodeRecord>()
        flatten(root, nodes, 0)

        val relevant = nodes
            .filter { it.isRelevant }
            .takeLast(MAX_RELEVANT_NODES)
            .map { it.debugLine() }

        val media = detectMedia(nodes)
        val progress = nodes.mapNotNull { parsePercent(it.text ?: it.contentDescription) }.maxOrNull()
        val hasDownloadActivity = nodes.any { it.looksDownloading }
        val hasScrollable = nodes.any {
            it.node.isScrollable && it.node.isVisibleToUser
        }
        val endHint = nodes.any { it.allText.containsAny(END_HINTS) }

        return TelegramUiSnapshot(
            packageName = root.packageName?.toString(),
            media = media,
            overallDownloadState = when {
                media.any { it.downloadState == DownloadState.DOWNLOADING } || hasDownloadActivity ->
                    DownloadState.DOWNLOADING
                media.any { it.downloadState == DownloadState.UNKNOWN } -> DownloadState.UNKNOWN
                media.any { it.downloadState == DownloadState.COMPLETE } -> DownloadState.COMPLETE
                else -> DownloadState.NONE
            },
            highestProgressPercent = progress,
            contentFingerprint = contentFingerprint(nodes),
            relevantNodes = relevant,
            isTelegramForeground = TELEGRAM_PACKAGES.contains(root.packageName?.toString()),
            hasScrollableContent = hasScrollable,
            endOfContentHint = endHint,
        )
    }

    private fun detectMedia(nodes: List<NodeRecord>): List<MediaItem> {
        val candidates = nodes.filter { it.looksLikeMedia && it.node.isVisibleToUser }
        val grouped = candidates.groupBy { mediaFingerprint(it) }
        return grouped.values.mapNotNull { group ->
            val best = group.maxByOrNull { it.area } ?: return@mapNotNull null
            val hasCompletionSignal = group.any { it.looksLikePlayControl || it.looksLikeLoadedImage }
            val signalText = group.joinToString(" ") {
                listOfNotNull(it.text, it.contentDescription).joinToString(" ")
            }
            val signal = downloadProgressDetector.inspect(
                text = signalText,
                className = best.className,
                hasCompletionEvidence = hasCompletionSignal,
            )

            MediaItem(
                fingerprint = mediaFingerprint(best),
                kind = best.mediaKind,
                bounds = best.bounds,
                text = best.text,
                contentDescription = best.contentDescription,
                downloadState = signal.state,
                progressPercent = signal.percent,
                completionEvidence = signal.evidence,
            )
        }
    }

    private fun flatten(
        node: AccessibilityNodeInfo,
        into: MutableList<NodeRecord>,
        depth: Int,
    ) {
        if (depth > MAX_DEPTH || into.size > MAX_NODES) return
        into += NodeRecord(node)
        for (index in 0 until node.childCount) {
            node.getChild(index)?.let { child ->
                flatten(child, into, depth + 1)
                child.recycle()
            }
        }
    }

    private fun contentFingerprint(nodes: List<NodeRecord>): String {
        val stable = nodes.asSequence()
            .filter { it.node.isVisibleToUser }
            .map {
                listOf(
                    it.className,
                    it.text.orEmpty(),
                    it.contentDescription.orEmpty(),
                    it.bounds.left / 8,
                    it.bounds.top / 8,
                    it.bounds.right / 8,
                    it.bounds.bottom / 8,
                ).joinToString("|")
            }
            .joinToString("||")
        return stable.sha256().take(18)
    }

    private fun mediaFingerprint(record: NodeRecord): String =
        listOf(
            record.className,
            record.viewId.orEmpty(),
            record.text.orEmpty(),
            record.contentDescription.orEmpty(),
            record.bounds.left / 12,
            record.bounds.top / 12,
            record.bounds.right / 12,
            record.bounds.bottom / 12,
        ).joinToString("|").sha256().take(24)

    private fun parsePercent(value: CharSequence?): Int? {
        val match = PERCENT_PATTERN.matcher(value?.toString().orEmpty())
        return if (match.find()) match.group(1)?.toIntOrNull()?.coerceIn(0, 100) else null
    }

    private data class NodeRecord(val node: AccessibilityNodeInfo) {
        val className: String = node.className?.toString().orEmpty()
        val viewId: String? = node.viewIdResourceName
        val text: String? = node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val contentDescription: String? =
            node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val bounds: Rect = Rect().also(node::getBoundsInScreen)
        val allText: String =
            listOfNotNull(text, contentDescription, viewId, className).joinToString(" ")
                .lowercase(Locale.US)
        val area: Int = bounds.width() * bounds.height()
        val mediaKind: String = when {
            allText.containsAny(VIDEO_HINTS) -> "video"
            allText.containsAny(IMAGE_HINTS) -> "image"
            else -> "media"
        }
        val looksLikeMedia: Boolean =
            className.containsAny(MEDIA_CLASSES) ||
                allText.containsAny(MEDIA_HINTS) ||
                (node.isClickable && node.childCount > 0 && area > MIN_MEDIA_AREA)
        val looksLikeLoadedImage: Boolean =
            className.containsAny(IMAGE_CLASSES) &&
                !allText.containsAny(DOWNLOAD_HINTS + LOADING_HINTS) &&
                area > MIN_MEDIA_AREA
        val looksLikePlayControl: Boolean =
            allText.containsAny(PLAY_HINTS) && (node.isClickable || className.contains("Image"))
        val looksLikeDownloadControl: Boolean =
            allText.containsAny(DOWNLOAD_HINTS)
        val looksDownloading: Boolean =
            looksLikeProgress ||
                allText.containsAny(DOWNLOADING_HINTS) ||
                PERCENT_PATTERN.matcher(allText).find()
        val looksLikeProgress: Boolean =
            className.containsAny(PROGRESS_CLASSES) ||
                allText.containsAny(PROGRESS_HINTS)
        val isRelevant: Boolean = looksLikeMedia || looksLikeProgress || looksLikeDownloadControl

        fun debugLine(): String =
            "${className.substringAfterLast('.')}" +
                " • ${contentDescription ?: text ?: viewId ?: "unnamed"}" +
                " • ${bounds.left},${bounds.top} ${bounds.width()}×${bounds.height()}"
    }

    private companion object {
        const val MAX_DEPTH = 32
        const val MAX_NODES = 2_500
        const val MAX_RELEVANT_NODES = 40
        const val MIN_MEDIA_AREA = 2_500
        val PERCENT_PATTERN = Pattern.compile("""(?<!\d)(\d{1,3})\s*%""")

        val TELEGRAM_PACKAGES = setOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.thunderdog.challegram",
        )
        val MEDIA_CLASSES = setOf("ImageView", "TextureView", "SurfaceView", "VideoView")
        val IMAGE_CLASSES = setOf("ImageView", "PhotoView")
        val PROGRESS_CLASSES = setOf("ProgressBar", "ProgressIndicator")
        val MEDIA_HINTS = setOf(
            "photo", "image", "video", "media", "thumbnail", "gallery", "attachment",
            "picture", "gif", "document", "camera",
        )
        val IMAGE_HINTS = setOf("photo", "image", "picture")
        val VIDEO_HINTS = setOf("video", "movie", "clip", "play")
        val PLAY_HINTS = setOf("play", "watch", "video")
        val DOWNLOAD_HINTS = setOf("download", "tap to download", "save")
        val DOWNLOADING_HINTS = setOf("downloading", "loading", "cancel download", "pause download")
        val LOADING_HINTS = setOf("loading", "download", "progress", "%")
        val PROGRESS_HINTS = setOf("progress", "loading", "downloading", "%")
        val END_HINTS = setOf("no more", "end of", "beginning", "nothing else")

        fun String.containsAny(values: Set<String>): Boolean =
            values.any { lowercase(Locale.US).contains(it) }

        fun String.sha256(): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}