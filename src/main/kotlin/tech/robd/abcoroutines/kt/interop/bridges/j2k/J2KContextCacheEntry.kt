// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/j2k/J2KContextCacheEntry.kt
// description: Internal data holder representing cached SuspendContext entries for J2K bridge in CACHED mode.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.j2k

import kotlinx.coroutines.Job
import tech.robd.jcoroutines.SuspendContext

/**
 * Cache entry for J2K CACHED mode.
 */
internal data class J2KContextCacheEntry(
    val context: SuspendContext,
    val kotlinJob: Job,
    val createdAt: Long = System.currentTimeMillis()
)
