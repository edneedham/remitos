package com.remitos.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DebugLogRetentionTest {
    @Test
    fun `excess logs returns zero when under limit`() {
        assertEquals(0, calculateExcessLogs(total = 120, max = 200))
    }

    @Test
    fun `excess logs returns difference when over limit`() {
        assertEquals(5, calculateExcessLogs(total = 205, max = 200))
    }
}
