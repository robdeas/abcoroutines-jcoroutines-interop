// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/ExposeAsKotlin.kt
// description: Wraps Java JCoroutine functions into Kotlin suspend functions (blocking/async/launch) with J2K bridge modes.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

@file:JvmName("ExposeAsKotlin")
package tech.robd.abcoroutines.kt.interop

import kotlinx.coroutines.Job
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import tech.robd.abcoroutines.ABCoroutines
import tech.robd.abcoroutines.kt.interop.bridges.j2k.J2KBridgeConfig
import tech.robd.abcoroutines.kt.interop.bridges.j2k.J2KBridgeMode
import tech.robd.abcoroutines.kt.interop.bridges.j2k.J2KContextCacheEntry
import tech.robd.abcoroutines.kt.interop.bridges.j2k.J2KDirectToken
import tech.robd.abcoroutines.kt.interop.bridges.j2k.J2KMinimalScope
import tech.robd.jcoroutines.JCoroutineScope
import tech.robd.jcoroutines.SuspendContext
import tech.robd.jcoroutines.internal.JCoroutineScopeImpl
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.coroutineContext

/**
 * ExposeAsKotlin: Convert Java JCoroutine functions to Kotlin suspend functions.
 * These methods create Kotlin-friendly suspend function wrappers around Java JCoroutine functions.
 */
object ExposeAsKotlin {

    // ========== JAVA BLOCKING FUNCTIONS -> KOTLIN SUSPEND ==========

    /** Convert (SuspendContext) -> R to suspend () -> R */
    /** Convert (SuspendContext, P1) -> R to suspend (P1) -> R */

    @JvmStatic
    @JvmSynthetic
    fun <R> blocking(javaFn: (SuspendContext) -> R): suspend () -> R = {
        val suspendContext = currentSuspendContext()
        withContext(ABCoroutines.VirtualThreads) {
            javaFn(suspendContext)
        }
    }

    @JvmStatic
    @JvmSynthetic
    fun <P1, R> blocking(javaFn: (SuspendContext, P1) -> R): suspend (P1) -> R = { p1 ->
        val suspendContext = currentSuspendContext()
        withContext(ABCoroutines.VirtualThreads) {
            javaFn(suspendContext, p1)
        }
    }

    /** Convert (SuspendContext, P1, P2) -> R to suspend (P1, P2) -> R */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, R> blocking(javaFn: (SuspendContext, P1, P2) -> R): suspend (P1, P2) -> R = { p1, p2 ->
        val suspendContext = currentSuspendContext()
        withContext(ABCoroutines.VirtualThreads) {
            javaFn(suspendContext, p1, p2)
        }
    }

    /** Convert (SuspendContext, P1, P2, P3) -> R to suspend (P1, P2, P3) -> R */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, P3, R> blocking(javaFn: (SuspendContext, P1, P2, P3) -> R): suspend (P1, P2, P3) -> R = { p1, p2, p3 ->
        val suspendContext = currentSuspendContext()
        withContext(ABCoroutines.VirtualThreads) {
            javaFn(suspendContext, p1, p2, p3)
        }
    }

    /** Convert (SuspendContext, P1, P2, P3, P4) -> R to suspend (P1, P2, P3, P4) -> R */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, P3, P4, R> blocking(javaFn: (SuspendContext, P1, P2, P3, P4) -> R): suspend (P1, P2, P3, P4) -> R =
        { p1, p2, p3, p4 ->
            val suspendContext = currentSuspendContext()
            withContext(ABCoroutines.VirtualThreads) {
                javaFn(suspendContext, p1, p2, p3, p4)
            }
        }

    // ========== JAVA ASYNC FUNCTIONS -> KOTLIN SUSPEND ==========

    /** Convert (SuspendContext) -> CompletableFuture<R> to suspend () -> R */
    @JvmStatic
    @JvmSynthetic
    fun <R> async0(javaFn: (SuspendContext) -> CompletableFuture<R>): suspend () -> R =
        { javaFn(currentSuspendContext()).await() }

    /** Convert (SuspendContext, P1) -> CompletableFuture<R> to suspend (P1) -> R */
    @JvmStatic
    @JvmSynthetic
    fun <P1, R> async1(javaFn: (SuspendContext, P1) -> CompletableFuture<R>): suspend (P1) -> R =
        { p1 -> javaFn(currentSuspendContext(), p1).await() }

    /** Convert (SuspendContext, P1, P2) -> CompletableFuture<R> to suspend (P1, P2) -> R */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, R> async2(javaFn: (SuspendContext, P1, P2) -> CompletableFuture<R>): suspend (P1, P2) -> R =
        { p1, p2 -> javaFn(currentSuspendContext(), p1, p2).await() }

    /** Convert (SuspendContext, P1, P2, P3) -> CompletableFuture<R> to suspend (P1, P2, P3) -> R */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, P3, R> async3(javaFn: (SuspendContext, P1, P2, P3) -> CompletableFuture<R>): suspend (P1, P2, P3) -> R =
        { p1, p2, p3 -> javaFn(currentSuspendContext(), p1, p2, p3).await() }

    /** Convert (SuspendContext, P1, P2, P3, P4) -> CompletableFuture<R> to suspend (P1, P2, P3, P4) -> R */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, P3, P4, R> async4(javaFn: (SuspendContext, P1, P2, P3, P4) -> CompletableFuture<R>): suspend (P1, P2, P3, P4) -> R =
        { p1, p2, p3, p4 -> javaFn(currentSuspendContext(), p1, p2, p3, p4).await() }

    // ========== JAVA LAUNCH FUNCTIONS -> KOTLIN SUSPEND ==========

    /** Convert (SuspendContext) -> Unit to suspend () -> Unit */
    @JvmStatic
    @JvmSynthetic
    fun launch0(javaFn: (SuspendContext) -> Unit): suspend () -> Unit =
        { javaFn(currentSuspendContext()) }

    /** Convert (SuspendContext, P1) -> Unit to suspend (P1) -> Unit */
    @JvmStatic
    @JvmSynthetic
    fun <P1> launch1(javaFn: (SuspendContext, P1) -> Unit): suspend (P1) -> Unit =
        { p1 -> javaFn(currentSuspendContext(), p1) }

    /** Convert (SuspendContext, P1, P2) -> Unit to suspend (P1, P2) -> Unit */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2> launch2(javaFn: (SuspendContext, P1, P2) -> Unit): suspend (P1, P2) -> Unit =
        { p1, p2 -> javaFn(currentSuspendContext(), p1, p2) }

    /** Convert (SuspendContext, P1, P2, P3) -> Unit to suspend (P1, P2, P3) -> Unit */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, P3> launch3(javaFn: (SuspendContext, P1, P2, P3) -> Unit): suspend (P1, P2, P3) -> Unit =
        { p1, p2, p3 -> javaFn(currentSuspendContext(), p1, p2, p3) }

    /** Convert (SuspendContext, P1, P2, P3, P4) -> Unit to suspend (P1, P2, P3, P4) -> Unit */
    @JvmStatic
    @JvmSynthetic
    fun <P1, P2, P3, P4> launch4(javaFn: (SuspendContext, P1, P2, P3, P4) -> Unit): suspend (P1, P2, P3, P4) -> Unit =
        { p1, p2, p3, p4 -> javaFn(currentSuspendContext(), p1, p2, p3, p4) }

    // ========== CORE UTILITIES ==========

    /**
     * Create a SuspendContext that bridges Kotlin coroutine cancellation to Java.
     * Mode is configurable for different performance/complexity tradeoffs.
     */
    private suspend fun currentSuspendContext(): SuspendContext {
        return when (J2KBridgeConfig.mode) {
            J2KBridgeMode.SIMPLE -> createSimpleBridgeContext()
            J2KBridgeMode.CACHED -> getCachedBridgeContext()
            J2KBridgeMode.PASSTHROUGH -> createPassthroughContext()
        }
    }

    private suspend fun createSimpleBridgeContext(): SuspendContext {
        val kotlinJob = coroutineContext[Job]
        val bridgedToken = J2KDirectToken(kotlinJob)
        val bridgeScope = createLightweightBridgeScope(kotlinJob)
        return SuspendContext.create(bridgeScope, bridgedToken)
    }

    /**
     * Cached mode: ThreadLocal caching for high-frequency bridge calls.
     * Good for request-scoped operations with many Java->Kotlin calls.
     */
    private suspend fun getCachedBridgeContext(): SuspendContext {
        val kotlinJob = coroutineContext[Job]
        val cached = j2kContextCache.get()

        if (kotlinJob?.isCancelled == true) {
            j2kContextCache.remove()
        }

        // Reuse if same job and context is still valid
        if (cached != null &&
            kotlinJob != null &&
            cached.kotlinJob === kotlinJob &&
            !cached.kotlinJob.isCancelled &&
            (System.currentTimeMillis() - cached.createdAt) < J2KBridgeConfig.cacheTTLMs
        ) {
            return cached.context
        }

        // Create new context - handle null job case
        val bridgedToken = J2KDirectToken(kotlinJob)
        val bridgeScope = createLightweightBridgeScope(kotlinJob)
        val newContext = SuspendContext.create(bridgeScope, bridgedToken)

        // Only cache if we have a valid job
        if (kotlinJob != null) {
            j2kContextCache.set(J2KContextCacheEntry(newContext, kotlinJob))

            // Clean up when job completes
            kotlinJob.invokeOnCompletion {
                j2kContextCache.remove()
            }
        }

        return newContext
    }

    /**
     * Passthrough mode: Minimal bridging with direct coroutine context access.
     * Most efficient for established patterns where bridge overhead is critical.
     */
    private suspend fun createPassthroughContext(): SuspendContext {
        val kotlinJob = coroutineContext[Job]
        val directToken = J2KDirectToken(kotlinJob)

        // Use minimal scope that delegates to current coroutine context
        val passthroughScope = createMinimalBridgeScope()

        return SuspendContext.create(passthroughScope, directToken)
    }

    /**
     * Create a lightweight bridge scope that delegates to ABCoroutines infrastructure
     * rather than creating new thread pools or heavy resources.
     */
    private fun createLightweightBridgeScope(kotlinJob: Job?): JCoroutineScope {
        // Use ABCoroutines' existing virtual thread infrastructure
        val bridgeScope = JCoroutineScopeImpl("j2k-bridge")

        // Optionally clean up when the Kotlin job completes
        // (left commented-out as in your source)
        // kotlinJob?.invokeOnCompletion { bridgeScope.close() }

        return bridgeScope
    }

    /**
     * Create minimal scope for passthrough mode - reuses existing infrastructure.
     */
    private fun createMinimalBridgeScope(): JCoroutineScope {
        // Return a scope that delegates to ABCoroutines without creating new resources
        return J2KMinimalScope(ABCoroutines.applicationScope)
    }

    // ThreadLocal cache for CACHED mode
    private val j2kContextCache = ThreadLocal<J2KContextCacheEntry?>()
}
