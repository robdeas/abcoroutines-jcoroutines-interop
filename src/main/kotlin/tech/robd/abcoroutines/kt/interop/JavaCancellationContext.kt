// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/JavaCancellationContext.kt
// description: CoroutineContext element bridging Java CancellationToken with Kotlin Job cancellation, ensuring bidirectional propagation.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop

import kotlinx.coroutines.*
import tech.robd.abcoroutines.ABCoroutines
import tech.robd.jcoroutines.CancellationToken
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * CoroutineContext element that bridges Java CancellationToken with Kotlin Job cancellation.
 * Ensures bidirectional cancellation propagation between Java and Kotlin coroutines.
 */
class JavaCancellationContext(
    private val javaCancellationToken: CancellationToken,
    private val kotlinJob: Job = SupervisorJob()
) : AbstractCoroutineContextElement(Key) {

    companion object Key : CoroutineContext.Key<JavaCancellationContext>

    private var cancelRegistration: AutoCloseable? = null

    init {
        // Java → Kotlin: When Java token is cancelled, cancel the Kotlin job
        cancelRegistration = javaCancellationToken.onCancel {
            kotlinJob.cancel(CancellationException("Java cancellation token was cancelled"))
        }

        // Kotlin → Java: When Kotlin job is cancelled, cancel the Java token
        kotlinJob.invokeOnCompletion { cause ->
            if (cause is CancellationException && !javaCancellationToken.isCancelled()) {
                javaCancellationToken.cancel()
            }
            // Clean up the registration
            try {
                cancelRegistration?.close()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }

        // If Java token is already cancelled, cancel the Kotlin job immediately
        if (javaCancellationToken.isCancelled()) {
            kotlinJob.cancel(CancellationException("Java cancellation token was already cancelled"))
        }
    }

    // Public API - delegate only what's needed
    val isActive: Boolean get() = kotlinJob.isActive
    val isCompleted: Boolean get() = kotlinJob.isCompleted
    val isCancelled: Boolean get() = kotlinJob.isCancelled

    // Allow access to the job for context composition
    internal val job: Job get() = kotlinJob

    fun cancel(cause: CancellationException? = null) {
        kotlinJob.cancel(cause)
    }

    suspend fun join() = kotlinJob.join()
}

/**
 * Helper function to create a coroutine context that bridges Java and Kotlin cancellation
 */
fun createJ2KBridgedContext(
    javaCancellationToken: CancellationToken,
    dispatcher: CoroutineDispatcher = ABCoroutines.VirtualThreads,
    parentJob: Job? = null
): CoroutineContext {
    val bridgeContext = JavaCancellationContext(
        javaCancellationToken,
        parentJob ?: SupervisorJob()
    )

    return dispatcher +
            CoroutineName("java-kotlin-bridge") +
            bridgeContext +
            ABCoroutines.defaultExceptionHandler
}
