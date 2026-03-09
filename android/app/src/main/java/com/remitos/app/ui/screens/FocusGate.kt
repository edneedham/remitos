package com.remitos.app.ui.screens

data class FocusGateConfig(
    val timeoutMs: Long = 1500L,
)

class FocusGate(
    private val config: FocusGateConfig = FocusGateConfig(),
) {
    private var focusStartedAt: Long = 0L
    private var focusConfirmed: Boolean = false

    fun reset(nowMs: Long) {
        focusStartedAt = nowMs
        focusConfirmed = false
    }

    fun onFocusResult(success: Boolean) {
        if (success) {
            focusConfirmed = true
        }
    }

    fun isReady(nowMs: Long): Boolean {
        return focusConfirmed || (nowMs - focusStartedAt) >= config.timeoutMs
    }
}
