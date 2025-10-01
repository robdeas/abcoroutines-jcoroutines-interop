// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/interop/bindings/KotlinBindings.kt
// description: Base class exposing Kotlin suspend functions to Java via typed function interfaces (blocking/async/launch).
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt.interop.bindings

import tech.robd.abcoroutines.kt.interop.ExposeAsJava
import tech.robd.jcoroutines.SuspendContext
import tech.robd.jcoroutines.fn.JCoroutineHandle
import tech.robd.jcoroutines.functiontypes.UnaryFunction
import tech.robd.jcoroutines.functiontypes.BinaryFunction
import tech.robd.jcoroutines.functiontypes.TriFunction
import tech.robd.jcoroutines.functiontypes.QuadFunction
import tech.robd.jcoroutines.functiontypes.QuinFunction

import java.util.concurrent.CompletableFuture

/**
 * Base class for exposing Kotlin suspend functions to Java.
 * Extend this to organize your Kotlin -> Java bindings cleanly.
 */
abstract class KotlinBindings {

    // [🧩 Section: api/blocking]
    // These create functions that block the current thread until completion

    protected fun <R : Any> blocking(kotlinFn: suspend () -> R): UnaryFunction<SuspendContext, R> =
        ExposeAsJava.blocking(kotlinFn)

    protected fun <P1 : Any, R : Any> blocking(kotlinFn: suspend (P1) -> R): BinaryFunction<SuspendContext, P1, R> =
        ExposeAsJava.blocking(kotlinFn)

    protected fun <P1 : Any, P2 : Any, R : Any> blocking(kotlinFn: suspend (P1, P2) -> R): TriFunction<SuspendContext, P1, P2, R> =
        ExposeAsJava.blocking(kotlinFn)

    protected fun <P1 : Any, P2 : Any, P3 : Any, R : Any> blocking(kotlinFn: suspend (P1, P2, P3) -> R): QuadFunction<SuspendContext, P1, P2, P3, R> =
        ExposeAsJava.blocking(kotlinFn)

    protected fun <P1 : Any, P2 : Any, P3 : Any, P4 : Any, R : Any> blocking(kotlinFn: suspend (P1, P2, P3, P4) -> R): QuinFunction<SuspendContext, P1, P2, P3, P4, R> =
        ExposeAsJava.blocking(kotlinFn)
    // [/🧩 Section: api/blocking]

    // [🧩 Section: api/async]
    // These create functions that return CompletableFuture for non-blocking execution

    protected fun <R> async(kotlinFn: suspend () -> R): UnaryFunction<SuspendContext, CompletableFuture<R>> =
        ExposeAsJava.async(kotlinFn)

    protected fun <P1 : Any, R> async(kotlinFn: suspend (P1) -> R): BinaryFunction<SuspendContext, P1, CompletableFuture<R>> =
        ExposeAsJava.async(kotlinFn)

    protected fun <P1 : Any, P2 : Any, R> async(kotlinFn: suspend (P1, P2) -> R): TriFunction<SuspendContext, P1, P2, CompletableFuture<R>> =
        ExposeAsJava.async(kotlinFn)

    protected fun <P1 : Any, P2 : Any, P3 : Any, R> async(kotlinFn: suspend (P1, P2, P3) -> R): QuadFunction<SuspendContext, P1, P2, P3, CompletableFuture<R>> =
        ExposeAsJava.async(kotlinFn)

    protected fun <P1 : Any, P2 : Any, P3 : Any, P4 : Any, R> async(kotlinFn: suspend (P1, P2, P3, P4) -> R): QuinFunction<SuspendContext, P1, P2, P3, P4, CompletableFuture<R>> =
        ExposeAsJava.async(kotlinFn)
    // [/🧩 Section: api/async]

    // [🧩 Section: api/launch]
    // These create functions that return JCoroutineHandle<Void> for cancellation/tracking

    protected fun launch(kotlinFn: suspend () -> Unit): UnaryFunction<SuspendContext, JCoroutineHandle<Void>> =
        ExposeAsJava.launch(kotlinFn)

    protected fun <P1 : Any> launch(kotlinFn: suspend (P1) -> Unit): BinaryFunction<SuspendContext, P1, JCoroutineHandle<Void>> =
        ExposeAsJava.launch(kotlinFn)

    protected fun <P1 : Any, P2 : Any> launch(kotlinFn: suspend (P1, P2) -> Unit): TriFunction<SuspendContext, P1, P2, JCoroutineHandle<Void>> =
        ExposeAsJava.launch(kotlinFn)

    protected fun <P1 : Any, P2 : Any, P3 : Any> launch(kotlinFn: suspend (P1, P2, P3) -> Unit): QuadFunction<SuspendContext, P1, P2, P3, JCoroutineHandle<Void>> =
        ExposeAsJava.launch(kotlinFn)

    protected fun <P1 : Any, P2 : Any, P3 : Any, P4 : Any> launch(kotlinFn: suspend (P1, P2, P3, P4) -> Unit): QuinFunction<SuspendContext, P1, P2, P3, P4, JCoroutineHandle<Void>> =
        ExposeAsJava.launch(kotlinFn)
    // [/🧩 Section: api/launch]
}
