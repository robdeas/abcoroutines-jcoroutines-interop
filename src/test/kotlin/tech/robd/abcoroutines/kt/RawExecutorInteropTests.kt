// [🧩 File Info]
// path: src/test/kotlin/tech/robd/abcoroutines/kt/RawExecutorInteropTests.kt
// description: Integration tests verifying ABCoroutines virtual-thread executor interop with raw CompletableFuture and cancellation bridging.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import tech.robd.abcoroutines.ABCoroutines
import tech.robd.abcoroutines.interop.ABCoroutinesInterop
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RawExecutorInteropTests {

    @AfterEach
    fun tearDownEach() {
        if (ABCoroutines.isRunning) ABCoroutines.shutdown()
    }

    @AfterAll
    fun tearDownAll() {
        ABCoroutines.reset(force = true)
    }

    // --- helper: await CF in a cancellable way (cancels CF if coroutine is cancelled)
    private suspend fun <T> CompletableFuture<T>.awaitCancellable(): T =
        suspendCancellableCoroutine { cont ->
            cont.invokeOnCancellation { this.cancel(true) }
            this.whenComplete { v, t ->
                if (t == null) cont.resume(v) else cont.resumeWithException(t)
            }
        }

    @Test
    fun `executor runs on virtual threads`() {
        ABCoroutines.reset(force = true)
        val exec = ABCoroutinesInterop.virtualThreadExecutor()

        val cf = CompletableFuture.supplyAsync(
            {
                // should be true on JDK virtual threads
                Thread.currentThread().isVirtual
            },
            exec
        )

        assertTrue(cf.get(2, TimeUnit.SECONDS))
    }

    @Test
    fun `kotlin and raw executor share the same VT lane`() = runTest {
        ABCoroutines.reset(force = true)
        val exec = ABCoroutinesInterop.virtualThreadExecutor()

        val k = async(ABCoroutines.VirtualThreads) {
            delay(30); "k"
        }

        val j = CompletableFuture.supplyAsync(
            { Thread.sleep(25); "j" },
            exec
        )

        assertEquals(listOf("k", "j"), listOf(k.await(), j.get(1, TimeUnit.SECONDS)))
    }

    @Test
    fun `cancelling coroutine cancels CompletableFuture via bridge`() = runTest {
        ABCoroutines.reset(force = true)
        val exec = ABCoroutinesInterop.virtualThreadExecutor()

        // Make the CF cooperative with cancellation (via interrupt checks)
        val cf = CompletableFuture.supplyAsync(
            {
                try {
                    var i = 0
                    while (true) {
                        if (Thread.currentThread().isInterrupted) {
                            throw CancellationException("Interrupted")
                        }
                        Thread.sleep(10)
                        if (++i > 500) break // safety
                    }
                    "finished" // should not reach if cancelled in time
                } catch (e: InterruptedException) {
                    throw CancellationException("InterruptedException").initCause(e)
                }
            },
            exec
        )

        val job = launch {
            // will cancel CF if this coroutine is cancelled
            cf.awaitCancellable()
        }

        // Give it a moment to start
        delay(30)
        job.cancel() // triggers cf.cancel(true) inside awaitCancellable()

        // CF should complete exceptionally with cancellation
        val thrown = runCatching { cf.get(1, TimeUnit.SECONDS) }.exceptionOrNull()
        assertNotNull(thrown)
        // Different JDKs wrap differently; accept either CancellationException or its wrapper
        assertTrue(
            thrown is java.util.concurrent.CancellationException ||
                    thrown!!.cause is java.util.concurrent.CancellationException
        )
    }

    @Test
    fun `after shutdown executor dispatch fails until reset`() {
        ABCoroutines.reset(force = true)

        val exec = ABCoroutinesInterop.virtualThreadExecutor()
        // First call should work
        assertEquals("ok", CompletableFuture.supplyAsync({ "ok" }, exec).get(1, TimeUnit.SECONDS))

        // Now shut down the core
        ABCoroutines.shutdown()

        // Submitting after shutdown should fail because VirtualThreads access triggers ensureRunning()
        val failed = runCatching {
            CompletableFuture.supplyAsync({ "nope" }, exec).get(1, TimeUnit.SECONDS)
        }.exceptionOrNull()

        assertNotNull(failed, "Submitting via shim after shutdown should fail")

        // Reset and verify we can run again
        ABCoroutines.reset(force = true)
        assertEquals("again", CompletableFuture.supplyAsync({ "again" }, exec).get(1, TimeUnit.SECONDS))
    }

    @Test
    fun `kotlin-only alias works for tests`() {
        ABCoroutines.reset(force = true)
        // Hidden from Java via @JvmSynthetic, but available here
        val exec = ABCoroutinesInterop.executor
        val cf = CompletableFuture.supplyAsync({ "alias" }, exec)
        assertEquals("alias", cf.get(1, TimeUnit.SECONDS))
    }
}
