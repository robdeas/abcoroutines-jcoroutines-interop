// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bridges/k2j/K2JBridgeConfig.kt
// description: Configuration object controlling Kotlin-to-Java (K2J) bridge mode and debug logging options.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bridges.k2j

/**
 * Configuration for the Kotlin-to-Java bridge behavior.
 * K2J has simpler configuration since most optimization happens in the Java execution context.
 */
object K2JBridgeConfig {
    /**
     * Current bridge mode. Change this to tune performance characteristics.
     * Default: SIMPLE (safest for transition scenarios)
     */
    var mode: K2JBridgeMode = K2JBridgeMode.SIMPLE

    /**
     * Enable debug logging for bridge operations.
     * Useful for understanding bridge behavior during development.
     */
    var debugLogging: Boolean = false

    /**
     * Configure bridge mode.
     */
    fun configure(mode: K2JBridgeMode) {
        this.mode = mode
    }

    /**
     * Reset to safe defaults.
     */
    fun reset() {
        mode = K2JBridgeMode.SIMPLE
        debugLogging = false
    }
}
