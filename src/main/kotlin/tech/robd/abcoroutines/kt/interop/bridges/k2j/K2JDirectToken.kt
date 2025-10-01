// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/k2j/K2JDirectToken.kt
// description: Direct cancellation token bridging Java CancellationToken to Kotlin Job for K2J minimal-overhead interop.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.k2j

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import tech.robd.jcoroutines.CancellationToken

/**
 * Direct cancellation token that bridges Java CancellationToken to Kotlin Job.
 * Used for minimal overhead scenarios.
 */
internal class K2JDirectToken(private val kotlinJob: Job) : CancellationToken {

    override fun isCancelled(): Boolean = kotlinJob.isCancelled

    override fun cancel(): Boolean {
        return if (!kotlinJob.isCancelled) {
            kotlinJob.cancel()
            true
        } else {
            false
        }
    }

    override fun onCancel(callback: Runnable): AutoCloseable {
        if (kotlinJob.isCancelled) {
            try {
                callback.run()
            } catch (_: Exception) {
            }
            return AutoCloseable { }
        }

        val handle = kotlinJob.invokeOnCompletion { cause ->
            if (cause is CancellationException) {
                try {
                    callback.run()
                } catch (_: Exception) {
                }
            }
        }

        return AutoCloseable { handle.dispose() }
    }

    override fun onCancelQuietly(callback: Runnable) {
        try {
            onCancel(callback)
        } catch (_: Exception) {
        }
    }

    override fun child(): CancellationToken {
        val childJob = Job(parent = kotlinJob)
        return K2JDirectToken(childJob)
    }
}
