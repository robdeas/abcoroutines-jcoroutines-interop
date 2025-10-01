// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/k2j/K2JBridgeMode.kt
// description: Enumeration defining Kotlin-to-Java (K2J) bridge modes for interop performance tuning and safety trade-offs.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.k2j

/**
 * Bridge modes for Kotlin-to-Java interop performance tuning.
 * K2J has fewer optimization opportunities since we're creating Java execution contexts.
 */
enum class K2JBridgeMode {
    /**
     * Safe default: Standard bridge with full context creation.
     * - Good for: Transition period, mixed usage patterns
     * - Performance: Standard overhead
     * - Safety: High - full isolation between contexts
     */
    SIMPLE,

    /**
     * Minimal overhead: Reuse existing infrastructure where possible.
     * - Good for: Established patterns, performance-critical paths
     * - Performance: Lower overhead
     * - Safety: High - minimal abstraction layers
     */
    MINIMAL
}
