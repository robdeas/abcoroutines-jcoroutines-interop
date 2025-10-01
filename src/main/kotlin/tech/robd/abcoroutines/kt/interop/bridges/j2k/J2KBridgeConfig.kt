// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/j2k/J2KBridgeConfig.kt
// description: Configuration object controlling Java-to-Kotlin (J2K) bridge mode, caching TTL, and debug logging options.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.j2k

/**
 * Configuration for the Java-to-Kotlin bridge behavior.
 * Allows tuning performance vs complexity tradeoffs for J2K calls.
 */
object J2KBridgeConfig {
    /**
     * Current bridge mode. Change this to tune performance characteristics.
     * Default: SIMPLE (safest for transition scenarios)
     */
    var mode: J2KBridgeMode = J2KBridgeMode.SIMPLE

    /**
     * Cache TTL in milliseconds for CACHED mode.
     * Contexts older than this are discarded and recreated.
     * Default: 30 seconds
     */
    var cacheTTLMs: Long = 30_000L

    /**
     * Enable debug logging for bridge operations.
     * Useful for understanding bridge behavior during development.
     */
    var debugLogging: Boolean = false

    /**
     * Configure bridge mode with TTL.
     */
    fun configure(mode: J2KBridgeMode, cacheTTLMs: Long = 30_000L) {
        this.mode = mode
        this.cacheTTLMs = cacheTTLMs
    }

    /**
     * Reset to safe defaults.
     */
    fun reset() {
        mode = J2KBridgeMode.SIMPLE
        cacheTTLMs = 30_000L
        debugLogging = false
    }
}
