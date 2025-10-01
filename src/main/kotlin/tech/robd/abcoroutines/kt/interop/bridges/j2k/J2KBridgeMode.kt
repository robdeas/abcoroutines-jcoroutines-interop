// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/j2k/J2KBridgeMode.kt
// description: Enumeration defining Java-to-Kotlin (J2K) bridge modes for interop performance tuning and safety trade-offs.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.j2k

/**
 * Bridge modes for Java-to-Kotlin interop performance tuning.
 * Choose based on your usage patterns and performance requirements.
 */
enum class J2KBridgeMode {
    /**
     * Safe default: Create fresh context each time.
     * - Good for: Transition period, mixed usage patterns
     * - Performance: Moderate overhead
     * - Safety: High - no caching complications
     */
    SIMPLE,

    /**
     * High-performance: ThreadLocal caching for repeated calls.
     * - Good for: Request-scoped operations, high-frequency bridge calls
     * - Performance: Low overhead for repeated calls
     * - Safety: Moderate - requires careful lifecycle management
     */
    CACHED,

    /**
     * Minimal overhead: Direct coroutine context bridging.
     * - Good for: Established patterns, performance-critical paths
     * - Performance: Lowest overhead
     * - Safety: High - minimal abstraction layers
     */
    PASSTHROUGH
}
