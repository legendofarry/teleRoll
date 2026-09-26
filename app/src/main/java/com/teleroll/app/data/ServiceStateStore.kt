package com.teleroll.app.data

import android.content.Context
import com.teleroll.app.automation.AutomationSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceStateStore private constructor() {
    private val _snapshot = MutableStateFlow(AutomationSnapshot())
    val snapshot: StateFlow<AutomationSnapshot> = _snapshot.asStateFlow()

    fun publish(snapshot: AutomationSnapshot) {
        _snapshot.value = snapshot
    }

    fun reset() {
        _snapshot.value = AutomationSnapshot()
    }

    companion object {
        @Volatile
        private var instance: ServiceStateStore? = null

        fun get(@Suppress("UNUSED_PARAMETER") context: Context): ServiceStateStore =
            instance ?: synchronized(this) {
                instance ?: ServiceStateStore().also { instance = it }
            }
    }
}