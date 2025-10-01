// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/j2k/J2KDirectToken.kt
// description: Direct cancellation token bridging Kotlin Job and Java CancellationToken for J2K PASSTHROUGH mode with minimal overhead.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.j2k

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import tech.robd.jcoroutines.CancellationToken
import tech.robd.jcoroutines.internal.CancellationTokenImpl

/**
 * Direct cancellation token for J2K PASSTHROUGH mode.
 * Provides minimal overhead bridging between Kotlin Job and Java CancellationToken.
 */
internal class J2KDirectToken(private val job: Job?) : CancellationToken {

    override fun isCancelled(): Boolean {
        return job?.isCancelled ?: false
    }

    override fun cancel(): Boolean {
        return if (job != null && !job.isCancelled) {
            job.cancel()
            true
        } else {
            false
        }
    }

    override fun onCancel(callback: Runnable): AutoCloseable {
        if (job == null) {
            return AutoCloseable { }
        }

        if (job.isCancelled) {
            try {
                callback.run()
            } catch (_: Exception) { }
            return AutoCloseable { }
        }

        // Register completion handler
        return try {
            val handle = job.invokeOnCompletion { cause ->
                val wasCancelled = job.isCancelled || cause is CancellationException
                if (wasCancelled) {
                    try {
                        callback.run()
                    } catch (_: Exception) { }
                }
            }

            AutoCloseable { handle?.dispose() }
        } catch (_: Exception) {
            // Fallback: polling mechanism if handler registration fails
            val poller = Thread.startVirtualThread {
                try {
                    while (!job.isCancelled && !Thread.currentThread().isInterrupted) {
                        Thread.sleep(50)
                    }
                    if (job.isCancelled) {
                        try {
                            callback.run()
                        } catch (_: Exception) { }
                    }
                } catch (_: InterruptedException) {
                    // Exit quietly
                }
            }
            AutoCloseable { poller.interrupt() }
        }
    }

    override fun onCancelQuietly(callback: Runnable) {
        try {
            onCancel(callback)
        } catch (_: Exception) { }
    }

    override fun child(): CancellationToken {
        return if (job != null) {
            val childJob = Job(parent = job)
            J2KDirectToken(childJob)
        } else {
            CancellationTokenImpl()
        }
    }
}
