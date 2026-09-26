package com.teleroll.app.tracker

import com.teleroll.app.automation.MediaItem

class MediaSessionTracker {
    private val processed = linkedSetOf<String>()

    fun isProcessed(item: MediaItem): Boolean = processed.contains(item.fingerprint)

    fun markProcessed(items: Collection<MediaItem>) {
        processed += items.map { it.fingerprint }
    }

    fun markProcessed(item: MediaItem) {
        processed += item.fingerprint
    }

    fun count(): Int = processed.size

    fun clear() {
        processed.clear()
    }
}