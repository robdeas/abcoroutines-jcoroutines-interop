// [🧩 File Info]
// path: src/test/kotlin/tech/robd/abcoroutines/kt/ABCoroutinesCancellationTests.kt
// description: Cancellation and lifecycle tests for ABCoroutines Virtual Threads integration.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]
package tech.robd.abcoroutines.kt

import tech.robd.abcoroutines.ABCoroutines
import tech.robd.abcoroutines.launchVT

import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ABCoroutinesCancellationTests {

    @AfterEach
    fun tearDownEach() {
        // ensure no leftover scope between tests
        if (ABCoroutines.isRunning) {
            ABCoroutines.shutdown()
        }
    }

    @AfterAll
    fun tearDown() {
        // important: we don’t want to shut down before other tests
        ABCoroutines.reset(force = true)
    }

    // [🧩 Region: Editable: tests/launchVT-cancels-with-parent]
    @Test
    fun `launchVT child cancels when parent scope is cancelled`() = runBlocking {
        ABCoroutines.reset(force = true)

        val started = CompletableDeferred<Unit>()
        val cancelledSeen = AtomicBoolean(false)

        val parent = ABCoroutines.newVirtualThreadScope()

        val job = parent.launchVT {
            started.complete(Unit)
            try {
                while (isActive) delay(100)
            } catch (ce: CancellationException) {
                cancelledSeen.set(true)
                throw ce
            }
        }

        withTimeout(2.seconds) { started.await() }

        parent.cancel("test cancel")
        withTimeout(2.seconds) { job.join() }

        assertTrue(job.isCancelled, "child job should be cancelled")
        assertTrue(cancelledSeen.get(), "child should have observed CancellationException")
    }
    // [/🧩 Region: Editable: tests/launchVT-cancels-with-parent]

    // [🧩 Region: Editable: tests/shutdown-cancels-and-prevents-reuse-until-reset]
    @Test
    fun `shutdown cancels running work and prevents reuse until reset`() = runBlocking {
        ABCoroutines.reset(force = true)

        val started = CompletableDeferred<Unit>()
        val stillRunning = AtomicBoolean(true)

        val job = ABCoroutines.launchVT {
            started.complete(Unit)
            try {
                while (isActive) delay(50)
            } finally {
                stillRunning.set(false)
            }
        }

        withTimeout(2.seconds) { started.await() }

        ABCoroutines.shutdown()

        withTimeout(2.seconds) { job.join() }
        assertTrue(job.isCancelled, "job should be cancelled by shutdown")
        assertFalse(stillRunning.get(), "job should have exited its loop")

        val thrown = runCatching {
            ABCoroutines.VirtualThreads // triggers ensureRunning()
        }.exceptionOrNull()

        assertNotNull(thrown, "VirtualThreads access should fail after shutdown")
        assertTrue(thrown is IllegalStateException)

        ABCoroutines.reset(force = true)
        val again = ABCoroutines.asyncVT { 42 }
        assertEquals(42, withTimeout(2.seconds) { again.await() })
    }
    // [/🧩 Region: Editable: tests/shutdown-cancels-and-prevents-reuse-until-reset]

    // [🧩 Region: Editable: tests/cancelling-asyncVT-propagates]
    @Test
    fun `cancelling asyncVT propagates CancellationException to awaiter`() = runBlocking {
        ABCoroutines.reset(force = true)

        val gate = CompletableDeferred<Unit>()
        val d = ABCoroutines.asyncVT {
            gate.complete(Unit)
            while (isActive) delay(100)
            1 // unreachable
        }

        withTimeout(2.seconds) { gate.await() }

        d.cancel(CancellationException("stop"))
        val ex = assertThrows(CancellationException::class.java) {
            runBlocking { d.await() }
        }

        assertTrue(d.isCancelled)
        assertTrue(ex.message?.contains("stop") == true)
    }
    // [/🧩 Region: Editable: tests/cancelling-asyncVT-propagates]

    // [🧩 Region: Editable: tests/reset-requires-shutdown]
    @Test
    fun `reset without shutdown throws unless force=true`() {
        ABCoroutines.reset(force = true)
        val ex = assertThrows(IllegalStateException::class.java) {
            ABCoroutines.reset(force = false)
        }
        assertTrue(ex.message!!.contains("shutdown() first"))
    }
    // [/🧩 Region: Editable: tests/reset-requires-shutdown]
}
