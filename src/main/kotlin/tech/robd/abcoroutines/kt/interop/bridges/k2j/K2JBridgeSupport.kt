// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/k2j/K2JBridgeSupport.kt
// description: Utility providing Kotlin-to-Java (K2J) bridged CoroutineContext creation for SIMPLE and MINIMAL modes with bidirectional cancellation.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.k2j

import kotlinx.coroutines.*
import tech.robd.abcoroutines.ABCoroutines
import tech.robd.jcoroutines.CancellationToken
import kotlin.coroutines.CoroutineContext

object K2JBridgeSupport {
    /**
     * Create a bridged context for K2J SIMPLE mode.
     * Standard context creation with full Java cancellation token integration.
     */
    fun createK2JBridgedContext(javaCancellationToken: CancellationToken): CoroutineContext {
        return createK2JContext(javaCancellationToken, "k2j-simple")
    }

    /**
     * Create a minimal context for K2J MINIMAL mode.
     * Lighter context creation while maintaining cancellation semantics.
     */
    fun createMinimalK2JContext(javaCancellationToken: CancellationToken): CoroutineContext {
        return createK2JContext(javaCancellationToken, "k2j-minimal", minimal = true)
    }

    /**
     * Core K2J context creation with optional minimal mode.
     */
    private fun createK2JContext(
        javaCancellationToken: CancellationToken,
        name: String,
        minimal: Boolean = false
    ): CoroutineContext {
        // Create Kotlin job that respects Java cancellation
        val kotlinJob = if (minimal) {
            // Minimal mode: simpler job creation
            Job()
        } else {
            // Standard mode: full supervisor job
            SupervisorJob()
        }

        // Set up bidirectional cancellation
        setupK2JCancellation(javaCancellationToken, kotlinJob)

        return ABCoroutines.VirtualThreads +
                CoroutineName(name) +
                kotlinJob +
                ABCoroutines.defaultExceptionHandler
    }

    /**
     * Set up bidirectional cancellation between Java CancellationToken and Kotlin Job.
     */
    private fun setupK2JCancellation(javaCancellationToken: CancellationToken, kotlinJob: Job) {
        // Java → Kotlin: When Java token is cancelled, cancel the Kotlin job
        javaCancellationToken.onCancel {
            kotlinJob.cancel(CancellationException("Java cancellation token was cancelled"))
        }

        // Kotlin → Java: When Kotlin job is cancelled, cancel the Java token
        kotlinJob.invokeOnCompletion { cause ->
            if (cause is CancellationException && !javaCancellationToken.isCancelled()) {
                javaCancellationToken.cancel()
            }
        }

        // If Java token is already cancelled, cancel the Kotlin job immediately
        if (javaCancellationToken.isCancelled()) {
            kotlinJob.cancel(CancellationException("Java cancellation token was already cancelled"))
        }
    }
}
