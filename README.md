# abcoroutines-jcoroutines-interop

> 🌉 A bridge layer for running JCoroutines (Java) and ABCoroutines (Kotlin) structured concurrency side-by-side in the same JVM — proving semantic equivalence and enabling safe incremental migration.

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/language-Kotlin-7F52FF.svg)](https://kotlinlang.org/)
[![JDK](https://img.shields.io/badge/JDK-21%2B-orange.svg)](https://openjdk.org/projects/jdk/21/)

**Related projects:**
[JCoroutines](https://github.com/robdeas/jcoroutines) · [ABCoroutines](https://github.com/robdeas/abcoroutines)

**Deep dive:** [The Cost of Concurrency Interop](https://robd.tech/abcoroutines-jcoroutines-interop/)

---

## What Is This?

[JCoroutines](https://github.com/robdeas/jcoroutines) is structured concurrency for Java 21+.  
[ABCoroutines](https://github.com/robdeas/abcoroutines) is the Kotlin counterpart — a façade over the same virtual-thread runtime with idiomatic Kotlin APIs.

Both implement the same structured-concurrency contract: scoped lifecycles, cooperative cancellation, timeout propagation, and explicit context passing. But proving they behave *identically* under load — cancellation, timing, error propagation — required more than reading the source.

This repository is the verification harness that does exactly that:

> Running two structured-concurrency engines inside the same JVM and confirming they behave as one.

It is intentionally **not a production library**. It is a diagnostic tool, a migration scaffold, and an architectural case study.

---

## Why It Exists

When both libraries shipped separately, the question was: do they actually propagate cancellation, timeouts, and errors the same way across complex coroutine hierarchies?

The only honest answer required a live test: make them cooperate inside a single runtime and watch what happens.

Three outcomes from doing this:

1. **Semantic parity confirmed** — cancellation tokens, timeouts, and scope propagation match exactly between the two runtimes.
2. **Incremental migration enabled** — Java JCoroutines code can be ported to Kotlin piece-by-piece, with the bridge verifying behaviour hasn't drifted.
3. **Architectural cost made visible** — shared runtime state collapses subsystem boundaries in ways that are fine for a scaffold and fatal for a long-lived system.

---

## The Bridge: How It Works

The core of the interop is a set of **functional adapters** that translate one runtime's entry points into the other's expectations — without reflection or hidden threading.

### Exposing a Kotlin suspend block as a Java callable

```kotlin
fun <T> exposeAsJava(block: suspend () -> T): UnaryFunction<Unit, T> =
    UnaryFunction { runBlocking { block() } }
```

### Exposing a Java-style JCoroutines block as a Kotlin suspend function

```kotlin
val javaFunction = { ctx: SuspendContext ->
    tokenRef.set(ctx.cancellationToken)
    if (!ready.isCompleted) ready.complete(Unit)
    "started"
}

val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
val result = runBlocking { kotlinSuspend() }
```

Here Kotlin is not just calling Java — it is *authoring* a JCoroutines-style coroutine, operating under the same structured-concurrency contract as the original Java runtime.

`ExposeAsKotlin` builds the wrapper. It does not run anything. You decide when and where to execute it.

---

## What the Tests Verify

The test suite exercises the bridge across both directions and validates semantic equivalence:

| Scenario | Verified |
|---|---|
| Cancellation token propagation across runtimes | ✅ |
| Timeout semantics from Java triggering Kotlin teardown | ✅ |
| Kotlin authoring and executing Java-style coroutines | ✅ |
| Scope lifecycle alignment (parent cancel → child cancel) | ✅ |
| Readiness signalling and join behaviour | ✅ |

Run the full suite:

```bash
./gradlew test
```

---

## Architectural Status

This bridge works exactly as intended. It is also **deliberately inappropriate for production**.

Connecting two structured-concurrency runtimes through shared state means:

- Subsystem boundaries collapse — ownership of execution context becomes ambiguous.
- Lifecycle coupling is implicit — both sides now know about each other's teardown.
- Circular adapter dependencies are easy to introduce — and they compile cleanly while silently destroying modular isolation.

The bridge is scaffolding. It belongs in:
- **Verification** — proving parity before a migration.
- **Transition** — porting Java components to Kotlin incrementally, with continuous behavioural validation.

It does not belong in:
- Long-running production services.
- Any architecture where clear subsystem ownership matters (which is most architectures).

> The insight is: shared efficiency costs modular independence. Every temporary bridge needs a demolition plan.

For a longer treatment of this design tension, see the [accompanying blog post](https://robd.tech/abcoroutines-jcoroutines-interop/).

---

## Prerequisites

- JDK 21+
- Gradle (wrapper included)
- [JCoroutines](https://github.com/robdeas/jcoroutines) and [ABCoroutines](https://github.com/robdeas/abcoroutines) on the classpath (see `build.gradle.kts`)

---

## Getting Started

```bash
git clone https://github.com/robdeas/abcoroutines-jcoroutines-interop.git
cd abcoroutines-jcoroutines-interop
./gradlew test
```

The test output will show parity assertions passing across both runtimes.

---

## Project Structure

```
src/
  main/kotlin/
    tech/robd/interop/
      ExposeAsKotlin.kt     # Wraps JCoroutines blocks as Kotlin suspend functions
      ExposeAsJava.kt       # Wraps Kotlin suspend blocks as Java UnaryFunction
  test/kotlin/
    tech/robd/interop/
      ...                   # Parity and lifecycle tests
```

---

## Related Projects

| Project | Description |
|---|---|
| [JCoroutines](https://github.com/robdeas/jcoroutines) | Structured concurrency for Java 21+ — no magic, explicit context passing |
| [ABCoroutines](https://github.com/robdeas/abcoroutines) | Kotlin façade over virtual threads — `parallel`, `race`, `retry`, channels |

---

## License

Copyright 2025 Rob Deas

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.

---

> 🧩 This repository is an architectural case study as much as a code artefact. The bridge works. The lesson is knowing when to dismantle it.
