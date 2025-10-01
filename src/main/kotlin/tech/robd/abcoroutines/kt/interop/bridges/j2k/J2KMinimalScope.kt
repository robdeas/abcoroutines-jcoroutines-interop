// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/j2k/J2KMinimalScope.kt
// description: Minimal J2K bridge scope that reuses an existing CoroutineScope for PASSTHROUGH mode.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.j2k

import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import tech.robd.abcoroutines.interop.ABCoroutinesInterop
import tech.robd.jcoroutines.JCoroutineScope
import tech.robd.jcoroutines.SuspendContext
import tech.robd.jcoroutines.SuspendFunction
import tech.robd.jcoroutines.SuspendRunnable
import tech.robd.jcoroutines.fn.JCoroutineHandle
import tech.robd.jcoroutines.internal.JCoroutineHandleImpl
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.coroutines.coroutineContext

/**
 * Minimal bridge scope for J2K PASSTHROUGH mode.
 * Delegates to existing CoroutineScope with minimal overhead.
 */
internal class J2KMinimalScope(
    private val kotlinScope: CoroutineScope
) : JCoroutineScope {

    override fun <T : Any> async(p0: SuspendFunction<T>): JCoroutineHandle<T> {
        val deferred = kotlinScope.async {
            val suspendContext = createMinimalSuspendContext()
            p0.apply(suspendContext)!!  // Force non-null since T : Any
        }

        val future = kotlinScope.future { deferred.await() }

        val token = J2KDirectToken(deferred)
        return JCoroutineHandleImpl(future, token)
    }


    override fun launch(block: SuspendRunnable): JCoroutineHandle<Void> {
        val job = kotlinScope.launch {
            val suspendContext = createMinimalSuspendContext()
            block.run(suspendContext)
        }

        val future = CompletableFuture<Void>()

        job.invokeOnCompletion { cause ->
            when {
                cause is CancellationException -> future.cancel(true)
                cause != null -> future.completeExceptionally(cause)
                else -> future.complete(null)
            }
        }

        val token = J2KDirectToken(job)
        return JCoroutineHandleImpl(future, token)
    }

    override fun <T> runBlocking(block: SuspendFunction<T>): T {
        return kotlinx.coroutines.runBlocking(kotlinScope.coroutineContext) {
            val suspendContext = createMinimalSuspendContext()
            val result = block.apply(suspendContext)

            // Handle the nullability mismatch between Java and Kotlin signatures
            if (result == null) {
                @Suppress("UNCHECKED_CAST")
                null as T  // If T is nullable, this works; if not, it will throw as expected
            } else {
                result
            }
        }
    }

    override fun <T> runBlocking(executor: Executor, block: SuspendFunction<T>): T {
        // For minimal bridge, ignore the executor parameter and use current context
        //return runBlocking(block)
        val dispatcher = executor.asCoroutineDispatcher()
        return kotlinx.coroutines.runBlocking(dispatcher) {
            val sc = createMinimalSuspendContext()
            @Suppress("UNCHECKED_CAST")
            block.apply(sc) as T
        }
    }

    override fun executor(): Executor {
        // Return ABCoroutines executor as fallback
        return ABCoroutinesInterop.executor
    }

    override fun close() {
        // Minimal scope doesn't own resources, so nothing to close
        // The underlying kotlinScope is managed externally
    }

    /**
     * Create minimal SuspendContext for passthrough operations.
     * Reuses existing job context without creating new scopes.
     */
    private suspend fun createMinimalSuspendContext(): SuspendContext {
        val currentJob = coroutineContext[Job.Key]
        val token = J2KDirectToken(currentJob)
        return SuspendContext.create(this, token)
    }
}
