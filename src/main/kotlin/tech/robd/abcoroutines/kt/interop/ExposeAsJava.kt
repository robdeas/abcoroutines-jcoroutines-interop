// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/ExposeAsJava.kt
// description: Converts Kotlin suspend functions into Java-callable coroutine function types (blocking, async, and launch variants) with bridge mode handling.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

@file:JvmName("ExposeAsJava")

package tech.robd.abcoroutines.kt.interop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import tech.robd.abcoroutines.ABCoroutines
import tech.robd.abcoroutines.kt.interop.bridges.k2j.K2JBridgeSupport.createK2JBridgedContext
import tech.robd.abcoroutines.kt.interop.bridges.k2j.K2JBridgeSupport.createMinimalK2JContext
import tech.robd.jcoroutines.SuspendContext
import tech.robd.jcoroutines.fn.JCoroutineHandle
import tech.robd.jcoroutines.functiontypes.*
import java.util.concurrent.CompletableFuture
import tech.robd.abcoroutines.kt.interop.bridges.k2j.K2JBridgeMode
import tech.robd.abcoroutines.kt.interop.bridges.k2j.K2JBridgeConfig

/**
 * ExposeAsJava: Convert Kotlin suspend functions to Java-callable JCoroutine functions.
 * These methods are visible to Java and create clean Java-friendly function signatures.
 */
object ExposeAsJava {

    // ========== KOTLIN SUSPEND -> BLOCKING JAVA FUNCTIONS ==========

    /** Convert suspend () -> R to UnaryFunction<SuspendContext, R> */
    @JvmStatic
    fun <R : Any> blocking(kotlinFn: suspend () -> R): UnaryFunction<SuspendContext, R> =
        UnaryFunction { suspendContext ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleBlocking(suspendContext) { kotlinFn() }
                K2JBridgeMode.MINIMAL -> minimalBlocking(suspendContext) { kotlinFn() }
            }
        }

    /** Convert suspend (P1) -> R to BinaryFunction<SuspendContext, P1, R> */
    @JvmStatic
    fun <P1 : Any, R : Any> blocking(kotlinFn: suspend (P1) -> R): BinaryFunction<SuspendContext, P1, R> =
        BinaryFunction { suspendContext, p1 ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleBlocking(suspendContext) { kotlinFn(p1) }
                K2JBridgeMode.MINIMAL -> minimalBlocking(suspendContext) { kotlinFn(p1) }
            }
        }

    /** Convert suspend (P1, P2) -> R to TriFunction<SuspendContext, P1, P2, R> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, R : Any> blocking(kotlinFn: suspend (P1, P2) -> R): TriFunction<SuspendContext, P1, P2, R> =
        TriFunction { suspendContext, p1, p2 ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleBlocking(suspendContext) { kotlinFn(p1, p2) }
                K2JBridgeMode.MINIMAL -> minimalBlocking(suspendContext) { kotlinFn(p1, p2) }
            }
        }

    /** Convert suspend (P1, P2, P3) -> R to QuadFunction<SuspendContext, P1, P2, P3, R> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, P3 : Any, R : Any> blocking(kotlinFn: suspend (P1, P2, P3) -> R): QuadFunction<SuspendContext, P1, P2, P3, R> =
        QuadFunction { suspendContext, a, b, c ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleBlocking(suspendContext) { kotlinFn(a, b, c) }
                K2JBridgeMode.MINIMAL -> minimalBlocking(suspendContext) { kotlinFn(a, b, c) }
            }
        }

    /** Convert suspend (P1, P2, P3, P4) -> R to QuinFunction<SuspendContext, P1, P2, P3, P4, R> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, P3 : Any, P4 : Any, R : Any> blocking(kotlinFn: suspend (P1, P2, P3, P4) -> R): QuinFunction<SuspendContext, P1, P2, P3, P4, R> =
        QuinFunction { suspendContext, a, b, c, d ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleBlocking(suspendContext) { kotlinFn(a, b, c, d) }
                K2JBridgeMode.MINIMAL -> minimalBlocking(suspendContext) { kotlinFn(a, b, c, d) }
            }
        }

    // ========== KOTLIN SUSPEND -> ASYNC JAVA FUNCTIONS ==========

    /** Convert suspend () -> R to UnaryFunction<SuspendContext, CompletableFuture<R>> */
    @JvmStatic
    fun <R> async(kotlinFn: suspend () -> R): UnaryFunction<SuspendContext, CompletableFuture<R>> =
        UnaryFunction { suspendContext ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleAsync(suspendContext) { kotlinFn() }
                K2JBridgeMode.MINIMAL -> minimalAsync(suspendContext) { kotlinFn() }
            }
        }

    /** Convert suspend (P1) -> R to BinaryFunction<SuspendContext, P1, CompletableFuture<R>> */
    @JvmStatic
    fun <P1 : Any, R> async(kotlinFn: suspend (P1) -> R): BinaryFunction<SuspendContext, P1, CompletableFuture<R>> =
        BinaryFunction { suspendContext, p1 ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleAsync(suspendContext) { kotlinFn(p1) }
                K2JBridgeMode.MINIMAL -> minimalAsync(suspendContext) { kotlinFn(p1) }
            }
        }

    /** Convert suspend (P1, P2) -> R to TriFunction<SuspendContext, P1, P2, CompletableFuture<R>> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, R> async(kotlinFn: suspend (P1, P2) -> R): TriFunction<SuspendContext, P1, P2, CompletableFuture<R>> =
        TriFunction { suspendContext, p1, p2 ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleAsync(suspendContext) { kotlinFn(p1, p2) }
                K2JBridgeMode.MINIMAL -> minimalAsync(suspendContext) { kotlinFn(p1, p2) }
            }
        }

    /** Convert suspend (P1, P2, P3) -> R to QuadFunction<SuspendContext, P1, P2, P3, CompletableFuture<R>> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, P3 : Any, R> async(kotlinFn: suspend (P1, P2, P3) -> R): QuadFunction<SuspendContext, P1, P2, P3, CompletableFuture<R>> =
        QuadFunction { suspendContext, a, b, c ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleAsync(suspendContext) { kotlinFn(a, b, c) }
                K2JBridgeMode.MINIMAL -> minimalAsync(suspendContext) { kotlinFn(a, b, c) }
            }
        }

    /** Convert suspend (P1, P2, P3, P4) -> R to QuinFunction<SuspendContext, P1, P2, P3, P4, CompletableFuture<R>> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, P3 : Any, P4 : Any, R> async(kotlinFn: suspend (P1, P2, P3, P4) -> R): QuinFunction<SuspendContext, P1, P2, P3, P4, CompletableFuture<R>> =
        QuinFunction { suspendContext, a, b, c, d ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleAsync(suspendContext) { kotlinFn(a, b, c, d) }
                K2JBridgeMode.MINIMAL -> minimalAsync(suspendContext) { kotlinFn(a, b, c, d) }
            }
        }

    // ========== KOTLIN SUSPEND -> LAUNCH JAVA FUNCTIONS ==========

    /** Convert suspend () -> Unit to UnaryFunction<SuspendContext, JCoroutineHandle<Void>> */
    @JvmStatic
    fun launch(kotlinFn: suspend () -> Unit): UnaryFunction<SuspendContext, JCoroutineHandle<Void>> =
        UnaryFunction { suspendContext ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleLaunch(suspendContext) { kotlinFn() }
                K2JBridgeMode.MINIMAL -> minimalLaunch(suspendContext) { kotlinFn() }
            }
        }

    /** Convert suspend (P1) -> Unit to BinaryFunction<SuspendContext, P1, JCoroutineHandle<Void>> */
    @JvmStatic
    fun <P1 : Any> launch(kotlinFn: suspend (P1) -> Unit): BinaryFunction<SuspendContext, P1, JCoroutineHandle<Void>> =
        BinaryFunction { suspendContext, p1 ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleLaunch(suspendContext) { kotlinFn(p1) }
                K2JBridgeMode.MINIMAL -> minimalLaunch(suspendContext) { kotlinFn(p1) }
            }
        }

    /** Convert suspend (P1, P2) -> Unit to TriFunction<SuspendContext, P1, P2, JCoroutineHandle<Void>> */
    @JvmStatic
    fun <P1 : Any, P2 : Any> launch(kotlinFn: suspend (P1, P2) -> Unit): TriFunction<SuspendContext, P1, P2, JCoroutineHandle<Void>> =
        TriFunction { suspendContext, p1, p2 ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleLaunch(suspendContext) { kotlinFn(p1, p2) }
                K2JBridgeMode.MINIMAL -> minimalLaunch(suspendContext) { kotlinFn(p1, p2) }
            }
        }

    /** Convert suspend (P1, P2, P3) -> Unit to QuadFunction<SuspendContext, P1, P2, P3, JCoroutineHandle<Void>> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, P3 : Any> launch(kotlinFn: suspend (P1, P2, P3) -> Unit): QuadFunction<SuspendContext, P1, P2, P3, JCoroutineHandle<Void>> =
        QuadFunction { suspendContext, a, b, c ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleLaunch(suspendContext) { kotlinFn(a, b, c) }
                K2JBridgeMode.MINIMAL -> minimalLaunch(suspendContext) { kotlinFn(a, b, c) }
            }
        }

    /** Convert suspend (P1, P2, P3, P4) -> Unit to QuinFunction<SuspendContext, P1, P2, P3, P4, JCoroutineHandle<Void>> */
    @JvmStatic
    fun <P1 : Any, P2 : Any, P3 : Any, P4 : Any> launch(kotlinFn: suspend (P1, P2, P3, P4) -> Unit): QuinFunction<SuspendContext, P1, P2, P3, P4, JCoroutineHandle<Void>> =
        QuinFunction { suspendContext, a, b, c, d ->
            when (K2JBridgeConfig.mode) {
                K2JBridgeMode.SIMPLE -> simpleLaunch(suspendContext) { kotlinFn(a, b, c, d) }
                K2JBridgeMode.MINIMAL -> minimalLaunch(suspendContext) { kotlinFn(a, b, c, d) }
            }
        }

    // ========== BRIDGE IMPLEMENTATION METHODS ==========

    private fun <R> simpleBlocking(suspendContext: SuspendContext, block: suspend () -> R): R {
        val bridgedContext = createK2JBridgedContext(suspendContext.cancellationToken)
        return kotlinx.coroutines.runBlocking(bridgedContext) { block() }
    }

    private fun <R> minimalBlocking(suspendContext: SuspendContext, block: suspend () -> R): R {
        // Minimal mode: less context creation overhead
        val bridgedContext = createMinimalK2JContext(suspendContext.cancellationToken)
        return kotlinx.coroutines.runBlocking(bridgedContext) { block() }
    }

    private fun <R> simpleAsync(suspendContext: SuspendContext, block: suspend () -> R): CompletableFuture<R> {
        val bridgedContext = createK2JBridgedContext(suspendContext.cancellationToken)
        val scope = CoroutineScope(bridgedContext)
        return scope.future(ABCoroutines.VirtualThreads) { block() }
    }

    private fun <R> minimalAsync(suspendContext: SuspendContext, block: suspend () -> R): CompletableFuture<R> {
        val bridgedContext = createMinimalK2JContext(suspendContext.cancellationToken)
        val scope = CoroutineScope(bridgedContext)
        return scope.future(ABCoroutines.VirtualThreads) { block() }
    }

    private fun simpleLaunch(suspendContext: SuspendContext, block: suspend () -> Unit): JCoroutineHandle<Void> {
        return suspendContext.launch { _ ->
            val bridgedContext = createK2JBridgedContext(suspendContext.cancellationToken)
            val scope = CoroutineScope(bridgedContext)
            scope.launch { block() }
        }
    }

    private fun minimalLaunch(suspendContext: SuspendContext, block: suspend () -> Unit): JCoroutineHandle<Void> {
        return suspendContext.launch { _ ->
            val bridgedContext = createMinimalK2JContext(suspendContext.cancellationToken)
            val scope = CoroutineScope(bridgedContext)
            scope.launch { block() }
        }
    }
}
