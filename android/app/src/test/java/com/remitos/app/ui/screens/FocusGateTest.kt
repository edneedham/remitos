package com.remitos.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusGateTest {
    @Test
    fun isReady_returnsTrueAfterTimeout() {
        val gate = FocusGate(FocusGateConfig(timeoutMs = 1000L))
        gate.reset(0L)

        assertFalse(gate.isReady(500L))
        assertTrue(gate.isReady(1000L))
        assertTrue(gate.isReady(1500L))
    }

    @Test
    fun isReady_returnsTrueWhenFocusSucceeds() {
        val gate = FocusGate(FocusGateConfig(timeoutMs = 5000L))
        gate.reset(0L)
        gate.onFocusResult(true)

        assertTrue(gate.isReady(10L))
    }

    @Test
    fun reset_clearsFocusSuccess() {
        val gate = FocusGate(FocusGateConfig(timeoutMs = 5000L))
        gate.reset(0L)
        gate.onFocusResult(true)
        assertTrue(gate.isReady(1L))

        gate.reset(100L)
        assertFalse(gate.isReady(101L))
    }
}
