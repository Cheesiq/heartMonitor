package com.heartmonitor.wear.monitoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MonitorUiState(
    val running: Boolean = false,
    val riskScore: Double = 0.0,
    val level: Int = 0,
    val status: String = "Idle",
    val lastRationale: List<String> = emptyList(),
    val sentAlerts: Int = 0
)

object AppState {
    private val mutableState = MutableStateFlow(MonitorUiState())
    val state: StateFlow<MonitorUiState> = mutableState.asStateFlow()

    fun update(transform: (MonitorUiState) -> MonitorUiState) {
        mutableState.value = transform(mutableState.value)
    }
}
