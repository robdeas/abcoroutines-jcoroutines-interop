// [🧩 File Info]
// path: src/main/kotlin/tech/robd/abcoroutines/kt/FunctionConvertors.kt
// description: Utility functions to convert between Java JCoroutine function types and Kotlin lambdas, maintaining clean interop.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

//@file:JvmName("FunctionConverters")
package tech.robd.abcoroutines.kt

import tech.robd.jcoroutines.functiontypes.*

/**
 * Utility class for converting between Java function types and Kotlin function types.
 * Keeps the Java interfaces clean while providing seamless interop in mixed codebases.
 */
object FunctionConverters {

    // ========== KOTLIN -> JAVA CONVERSIONS ==========

    /** Convert () -> R to NullaryFunction<R> */
    @JvmStatic
    fun <R : Any> fromKotlin(kotlinFn: () -> R): NullaryFunction<R> =
        NullaryFunction { kotlinFn() }

    /** Convert (T) -> R to UnaryFunction<T, R> */
    @JvmStatic
    fun <T : Any, R : Any> fromKotlin(kotlinFn: (T) -> R): UnaryFunction<T, R> =
        UnaryFunction { t -> kotlinFn(t) }

    /** Convert (T, U) -> R to BinaryFunction<T, U, R> */
    @JvmStatic
    fun <T : Any, U : Any, R : Any> fromKotlin(kotlinFn: (T, U) -> R): BinaryFunction<T, U, R> =
        BinaryFunction { t, u -> kotlinFn(t, u) }

    /** Convert (T, U, V) -> R to TriFunction<T, U, V, R> */
    @JvmStatic
    fun <T : Any, U : Any, V : Any, R : Any> fromKotlin(kotlinFn: (T, U, V) -> R): TriFunction<T, U, V, R> =
        TriFunction { t, u, v -> kotlinFn(t, u, v) }

    /** Convert (T, U, V, W) -> R to QuadFunction<T, U, V, W, R> */
    @JvmStatic
    fun <T : Any, U : Any, V : Any, W : Any, R : Any> fromKotlin(kotlinFn: (T, U, V, W) -> R): QuadFunction<T, U, V, W, R> =
        QuadFunction { t, u, v, w -> kotlinFn(t, u, v, w) }

    /** Convert (T, U, V, W, X) -> R to QuinFunction<T, U, V, W, X, R> */
    @JvmStatic
    fun <T : Any, U : Any, V : Any, W : Any, X : Any, R : Any> fromKotlin(kotlinFn: (T, U, V, W, X) -> R): QuinFunction<T, U, V, W, X, R> =
        QuinFunction { t, u, v, w, x -> kotlinFn(t, u, v, w, x) }

    // ========== JAVA -> KOTLIN CONVERSIONS ==========

    /** Convert NullaryFunction<R> to () -> R */
    @JvmStatic
    fun <R : Any> toKotlin(javaFn: NullaryFunction<R>): () -> R =
        { javaFn.get() }

    /** Convert UnaryFunction<T, R> to (T) -> R */
    @JvmStatic
    fun <T : Any, R : Any> toKotlin(javaFn: UnaryFunction<T, R>): (T) -> R =
        { t -> javaFn.apply(t) }

    /** Convert BinaryFunction<T, U, R> to (T, U) -> R */
    @JvmStatic
    fun <T : Any, U : Any, R : Any> toKotlin(javaFn: BinaryFunction<T, U, R>): (T, U) -> R =
        { t, u -> javaFn.apply(t, u) }

    /** Convert TriFunction<T, U, V, R> to (T, U, V) -> R */
    @JvmStatic
    fun <T : Any, U : Any, V : Any, R : Any> toKotlin(javaFn: TriFunction<T, U, V, R>): (T, U, V) -> R =
        { t, u, v -> javaFn.apply(t, u, v) }

    /** Convert QuadFunction<T, U, V, W, R> to (T, U, V, W) -> R */
    @JvmStatic
    fun <T : Any, U : Any, V : Any, W : Any, R : Any> toKotlin(javaFn: QuadFunction<T, U, V, W, R>): (T, U, V, W) -> R =
        { t, u, v, w -> javaFn.apply(t, u, v, w) }

    /** Convert QuinFunction<T, U, V, W, X, R> to (T, U, V, W, X) -> R */
    @JvmStatic
    fun <T : Any, U : Any, V : Any, W : Any, X : Any, R : Any> toKotlin(javaFn: QuinFunction<T, U, V, W, X, R>): (T, U, V, W, X) -> R =
        { t, u, v, w, x -> javaFn.apply(t, u, v, w, x) }
}

// ========== KOTLIN EXTENSION FUNCTIONS ==========

/**
 * Extension functions for more idiomatic Kotlin usage.
 * These are @JvmSynthetic to hide from Java callers.
 */

// Java -> Kotlin extensions
@JvmSynthetic
fun <R : Any> NullaryFunction<R>.asKotlin(): () -> R = FunctionConverters.toKotlin(this)

@JvmSynthetic
fun <T : Any, R : Any> UnaryFunction<T, R>.asKotlin(): (T) -> R = FunctionConverters.toKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, R : Any> BinaryFunction<T, U, R>.asKotlin(): (T, U) -> R = FunctionConverters.toKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, V : Any, R : Any> TriFunction<T, U, V, R>.asKotlin(): (T, U, V) -> R = FunctionConverters.toKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, V : Any, W : Any, R : Any> QuadFunction<T, U, V, W, R>.asKotlin(): (T, U, V, W) -> R = FunctionConverters.toKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, V : Any, W : Any, X : Any, R : Any> QuinFunction<T, U, V, W, X, R>.asKotlin(): (T, U, V, W, X) -> R = FunctionConverters.toKotlin(this)

// Kotlin -> Java extensions
@JvmSynthetic
fun <R : Any> (() -> R).asJava(): NullaryFunction<R> = FunctionConverters.fromKotlin(this)

@JvmSynthetic
fun <T : Any, R : Any> ((T) -> R).asJava(): UnaryFunction<T, R> = FunctionConverters.fromKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, R : Any> ((T, U) -> R).asJava(): BinaryFunction<T, U, R> = FunctionConverters.fromKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, V : Any, R : Any> ((T, U, V) -> R).asJava(): TriFunction<T, U, V, R> = FunctionConverters.fromKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, V : Any, W : Any, R : Any> ((T, U, V, W) -> R).asJava(): QuadFunction<T, U, V, W, R> = FunctionConverters.fromKotlin(this)

@JvmSynthetic
fun <T : Any, U : Any, V : Any, W : Any, X : Any, R : Any> ((T, U, V, W, X) -> R).asJava(): QuinFunction<T, U, V, W, X, R> = FunctionConverters.fromKotlin(this)
