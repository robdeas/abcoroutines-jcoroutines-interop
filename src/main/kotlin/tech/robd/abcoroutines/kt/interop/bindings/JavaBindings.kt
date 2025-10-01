// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bindings/JavaBindings.kt
// description: Base class exposing Java JCoroutine suspend functions to Kotlin
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bindings

import tech.robd.abcoroutines.kt.interop.ExposeAsKotlin
import tech.robd.jcoroutines.SuspendContext
import java.util.concurrent.CompletableFuture

/**
 * Base class for exposing Java JCoroutine functions to Kotlin.
 * Extend this to organize your Java -> Kotlin bindings cleanly.
 */
abstract class JavaBindings {

    // Helper methods using ExposeAsKotlin - these create Kotlin suspend functions
    // Note: @JvmSynthetic methods are hidden from Java callers

    protected fun <R> blocking(javaFn: (SuspendContext) -> R) = ExposeAsKotlin.blocking(javaFn)
    protected fun <P1, R> blocking(javaFn: (SuspendContext, P1) -> R) = ExposeAsKotlin.blocking(javaFn)
    protected fun <P1, P2, R> blocking(javaFn: (SuspendContext, P1, P2) -> R) = ExposeAsKotlin.blocking(javaFn)
    protected fun <P1, P2, P3, R> blocking(javaFn: (SuspendContext, P1, P2, P3) -> R) = ExposeAsKotlin.blocking(javaFn)
    protected fun <P1, P2, P3, P4, R> blocking(javaFn: (SuspendContext, P1, P2, P3, P4) -> R) = ExposeAsKotlin.blocking(javaFn)

    protected fun <R> async(javaFn: (SuspendContext) -> CompletableFuture<R>) = ExposeAsKotlin.async0(javaFn)
    protected fun <P1, R> async(javaFn: (SuspendContext, P1) -> CompletableFuture<R>) = ExposeAsKotlin.async1(javaFn)
    protected fun <P1, P2, R> async(javaFn: (SuspendContext, P1, P2) -> CompletableFuture<R>) = ExposeAsKotlin.async2(javaFn)
    protected fun <P1, P2, P3, R> async(javaFn: (SuspendContext, P1, P2, P3) -> CompletableFuture<R>) = ExposeAsKotlin.async3(javaFn)
    protected fun <P1, P2, P3, P4, R> async(javaFn: (SuspendContext, P1, P2, P3, P4) -> CompletableFuture<R>) = ExposeAsKotlin.async4(javaFn)

    protected fun launch(javaFn: (SuspendContext) -> Unit) = ExposeAsKotlin.launch0(javaFn)
    protected fun <P1> launch(javaFn: (SuspendContext, P1) -> Unit) = ExposeAsKotlin.launch1(javaFn)
    protected fun <P1, P2> launch(javaFn: (SuspendContext, P1, P2) -> Unit) = ExposeAsKotlin.launch2(javaFn)
    protected fun <P1, P2, P3> launch(javaFn: (SuspendContext, P1, P2, P3) -> Unit) = ExposeAsKotlin.launch3(javaFn)
    protected fun <P1, P2, P3, P4> launch(javaFn: (SuspendContext, P1, P2, P3, P4) -> Unit) = ExposeAsKotlin.launch4(javaFn)
}

