package com.fitness.restlock.backend.blocking

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class BlockingDiagnosticsState(
    val lockActive: Boolean = false,
    val lastDetectedPackage: String? = null,
    val lastBlockedPackage: String? = null,
)

object BlockingDiagnostics {
    private val _state = MutableStateFlow(BlockingDiagnosticsState())
    val state: StateFlow<BlockingDiagnosticsState> = _state

    fun reset() {
        _state.value = BlockingDiagnosticsState()
    }

    fun updateLockActive(lockActive: Boolean) {
        _state.update { it.copy(lockActive = lockActive) }
    }

    fun updateLastDetected(packageName: String) {
        _state.update { it.copy(lastDetectedPackage = packageName) }
    }

    fun updateLastBlocked(packageName: String) {
        _state.update { it.copy(lastBlockedPackage = packageName) }
    }
}
