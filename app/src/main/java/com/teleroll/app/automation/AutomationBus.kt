package com.teleroll.app.automation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AutomationBus {
    private val _snapshot = MutableStateFlow(AutomationSnapshot())
    val snapshot: StateFlow<AutomationSnapshot> = _snapshot.asStateFlow()

    fun publish(snapshot: AutomationSnapshot) {
        _snapshot.value = snapshot
    }
}