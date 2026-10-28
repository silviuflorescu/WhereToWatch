package com.coroutine.mastery.wheretowatch.core.util

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuspendRunCatchingTest {

    @Test
    fun `returns success`() {
        assertEquals(Result.success(42), suspendRunCatching { 42 })
    }

    @Test
    fun `wraps regular exceptions in a failure`() {
        val result = suspendRunCatching { error("boom") }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test(expected = CancellationException::class)
    fun `rethrows CancellationException`() {
        suspendRunCatching { throw CancellationException("cancelled") }
    }
}
