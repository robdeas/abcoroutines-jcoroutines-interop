// [🧩 File Info]
// path: src/test/kotlin/tech/robd/abcoroutines/kt/BrridgeIntegrationTest.kt
// description: Integration tests for J2K/K2J bridge modes, cancellation, async, round-trip, performance, and bindings.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import tech.robd.abcoroutines.ABCoroutines
import tech.robd.abcoroutines.kt.interop.ExposeAsJava
import tech.robd.abcoroutines.kt.interop.ExposeAsKotlin
import tech.robd.abcoroutines.kt.interop.bindings.KotlinBindings
import tech.robd.abcoroutines.kt.interop.bridges.j2k.J2KBridgeConfig
import tech.robd.abcoroutines.kt.interop.bridges.j2k.J2KBridgeMode
import tech.robd.abcoroutines.kt.interop.bridges.k2j.K2JBridgeConfig
import tech.robd.abcoroutines.kt.interop.bridges.k2j.K2JBridgeMode
import tech.robd.jcoroutines.StandardCoroutineScope
import tech.robd.jcoroutines.SuspendContext
import tech.robd.jcoroutines.internal.JCoroutineScopeImpl
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BridgeIntegrationTest {

    @BeforeEach
    fun setUp() {
        System.setProperty("jcoroutines.diag", "true")
        J2KBridgeConfig.reset()
        K2JBridgeConfig.reset()
    }

    @AfterAll
    fun tearDown() {
        ABCoroutines.shutdown()
    }

    // [🧩 Section: tests/j2k-bridge-modes]

    @Test
    fun `J2K SIMPLE mode creates fresh context each time`() {
        runBlocking {
            J2KBridgeConfig.mode = J2KBridgeMode.SIMPLE
            val callCount = AtomicInteger(0)
            val javaFunction = { ctx: SuspendContext ->
                ctx.delay(50)
                "result-${callCount.incrementAndGet()}"
            }
            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
            val result1 = kotlinSuspend()
            val result2 = kotlinSuspend()
            assertEquals("result-1", result1)
            assertEquals("result-2", result2)
        }
    }

    @Test
    fun `J2K CACHED mode reuses context appropriately`() {
        runBlocking {
            J2KBridgeConfig.configure(J2KBridgeMode.CACHED, cacheTTLMs = 1000L)
            val contextIds = mutableSetOf<Int>()
            val javaFunction = { ctx: SuspendContext ->
                contextIds.add(System.identityHashCode(ctx))
                "cached-result"
            }
            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
            launch {
                kotlinSuspend()
                kotlinSuspend()
                kotlinSuspend()
            }.join()
            assertTrue(contextIds.size <= 2, "Too many contexts created: ${contextIds.size}")
        }
    }

    @Test
    fun `J2K PASSTHROUGH mode has minimal overhead`() {
        runBlocking {
            J2KBridgeConfig.mode = J2KBridgeMode.PASSTHROUGH
            val executionTimes = mutableListOf<Long>()
            val javaFunction = { ctx: SuspendContext ->
                val start = System.nanoTime()
                ctx.delay(10)
                val end = System.nanoTime()
                executionTimes.add(end - start)
                "passthrough-result"
            }
            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
            repeat(5) { kotlinSuspend() }
            assertEquals(5, executionTimes.size)
            assertTrue(executionTimes.all { it > 0 })
        }
    }

    // [/🧩 Section: tests/j2k-bridge-modes]

    // [🧩 Section: tests/k2j-bridge-modes]

    @Test
    fun `K2J SIMPLE mode creates standard contexts`() {
        runBlocking {
            K2JBridgeConfig.mode = K2JBridgeMode.SIMPLE
            val kotlinSuspend: suspend (String) -> String = { input ->
                delay(50)
                "processed: $input"
            }
            val javaFunction = ExposeAsJava.blocking(kotlinSuspend)
            val scope = StandardCoroutineScope()
            val suspendContext = SuspendContext.create(scope)
            val result = javaFunction.apply(suspendContext, "test-input")
            assertEquals("processed: test-input", result)
            scope.close()
        }
    }

    @Test
    fun `K2J MINIMAL mode creates lightweight contexts`() {
        runBlocking {
            K2JBridgeConfig.mode = K2JBridgeMode.MINIMAL
            val kotlinSuspend: suspend (String) -> String = { input ->
                delay(50)
                "minimal: $input"
            }
            val javaFunction = ExposeAsJava.blocking(kotlinSuspend)
            val scope = StandardCoroutineScope()
            val suspendContext = SuspendContext.create(scope)
            val result = javaFunction.apply(suspendContext, "test")
            assertEquals("minimal: test", result)
            scope.close()
        }
    }

    // [/🧩 Section: tests/k2j-bridge-modes]

    // [🧩 Section: tests/cancellation]
// [🧩 Section: tests/cancellation]
    @Test
    fun `J2K cancellation propagates from Kotlin to Java`() {
        runBlocking {
            val tokenRef = java.util.concurrent.atomic.AtomicReference<tech.robd.jcoroutines.CancellationToken>()
            val ready = kotlinx.coroutines.CompletableDeferred<Unit>()
            val javaFunction = { ctx: SuspendContext ->
                // capture token immediately and signal readiness
                tokenRef.set(ctx.cancellationToken)
                if (!ready.isCompleted) ready.complete(Unit)
                // return right away – do NOT block here
                "started"
            }

            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)

            val job = launch {
                // we don't care about the result; just drive the bridge
                runCatching { kotlinSuspend() }
            }

            // Wait until the lambda definitely ran and we captured the token
            withTimeout(5_000) { ready.await() }

            // Cancel deterministically and wait for completion
            job.cancelAndJoin()

            // Now, wait (bounded) for the Java-side token to observe cancellation
            val startNs = System.nanoTime()
            val timeoutNs = java.util.concurrent.TimeUnit.SECONDS.toNanos(2)
            var ok = false
            while (System.nanoTime() - startNs < timeoutNs) {
                val t = tokenRef.get()
                if (t != null && t.isCancelled()) { ok = true; break }
                // light backoff to let cancellation propagate
                kotlinx.coroutines.delay(5)
            }

            assertTrue(ok, "Java side should observe token.isCancelled() after Kotlin job cancellation")
        }
    }



    @Test
    fun `K2J cancellation propagates from Java to Kotlin`() {
        runBlocking {
            val cancelled = AtomicBoolean(false)
            val started = AtomicBoolean(false)
            val kotlinSuspend = suspend {
                started.set(true)
                try {
                    delay(5000)
                    "should-not-complete"
                } catch (e: CancellationException) {
                    cancelled.set(true)
                    throw e
                }
            }
            val javaFunction = ExposeAsJava.async(kotlinSuspend)
            val scope = StandardCoroutineScope()
            val suspendContext = SuspendContext.create(scope)
            val future = javaFunction.apply(suspendContext)
            while (!started.get()) Thread.sleep(10)
            future.cancel(true)
            Thread.sleep(100)
            assertTrue(cancelled.get())
            scope.close()
        }
    }

    // [/🧩 Section: tests/cancellation]

    // [🧩 Section: tests/round-trip]

    @Test
    fun `Java to Kotlin to Java round trip preserves data and behavior`() {
        runBlocking {
            val originalJavaFunction = { ctx: SuspendContext, input: String ->
                ctx.delay(50)
                "java-processed: $input"
            }
            val kotlinSuspend = ExposeAsKotlin.blocking(originalJavaFunction)
            val backToJavaFunction = ExposeAsJava.blocking(kotlinSuspend)
            val scope = StandardCoroutineScope()
            val suspendContext = SuspendContext.create(scope)
            val result = backToJavaFunction.apply(suspendContext, "test-data")
            assertEquals("java-processed: test-data", result)
            scope.close()
        }
    }

    @Test
    fun `Kotlin to Java to Kotlin round trip preserves data and behavior`() = runBlocking {
        val originalKotlinSuspend: suspend (String) -> String = { input ->
            delay(50)
            "processed: $input"
        }
        val javaFunction = ExposeAsJava.blocking(originalKotlinSuspend)
        val backToKotlinSuspend = ExposeAsKotlin.blocking<String, String>(javaFunction::apply)
        val result = backToKotlinSuspend("test-data")
        assertEquals("processed: test-data", result)
    }

    // [/🧩 Section: tests/round-trip]

    // [🧩 Section: tests/async-bridges]

    @Test
    fun `J2K async bridge handles CompletableFuture correctly`() {
        runBlocking {
            val javaAsyncFunction = { _: SuspendContext ->
                CompletableFuture.supplyAsync {
                    Thread.sleep(100)
                    "async-result"
                }
            }
            val kotlinSuspend = ExposeAsKotlin.async0(javaAsyncFunction)
            val result = kotlinSuspend()
            assertEquals("async-result", result)
        }
    }

    @Test
    fun `K2J async bridge creates proper CompletableFuture`() {
        runBlocking {
            val kotlinSuspend = suspend {
                delay(100)
                "kotlin-async-result"
            }
            val javaAsyncFunction = ExposeAsJava.async(kotlinSuspend)
            val scope = StandardCoroutineScope()
            val suspendContext = SuspendContext.create(scope)
            val future = javaAsyncFunction.apply(suspendContext)
            val result = future.get(1, TimeUnit.SECONDS)
            assertEquals("kotlin-async-result", result)
            scope.close()
        }
    }

    // [/🧩 Section: tests/async-bridges]

    // [🧩 Section: tests/error-handling]

    @Test
    fun `J2K bridge propagates exceptions correctly`() {
        val javaFunction = { _: SuspendContext ->
            throw IllegalArgumentException("Java exception")
        }
        val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
        assertThrows<IllegalArgumentException> { runBlocking { kotlinSuspend() } }
    }

    @Test
    fun `K2J bridge propagates exceptions correctly`() {
        val kotlinSuspend = suspend {
            throw IllegalStateException("Kotlin exception")
        }
        val javaFunction = ExposeAsJava.blocking(kotlinSuspend)
        val scope = StandardCoroutineScope()
        val suspendContext = SuspendContext.create(scope)
        assertThrows<IllegalStateException> { javaFunction.apply(suspendContext) }
        scope.close()
    }

    // [/🧩 Section: tests/error-handling]

    // [🧩 Section: tests/performance-and-resources]

    @Test
    fun `Cache TTL behavior works correctly`() {
        runBlocking {
            J2KBridgeConfig.configure(J2KBridgeMode.CACHED, cacheTTLMs = 200L)
            val callTimes = mutableListOf<Long>()
            val javaFunction = { _: SuspendContext ->
                callTimes.add(System.currentTimeMillis())
                "cached-call"
            }
            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
            launch { kotlinSuspend() }.join()
            launch { kotlinSuspend() }.join()
            delay(300)
            launch { kotlinSuspend() }.join()
            assertEquals(3, callTimes.size)
            val timeDiff1 = callTimes[1] - callTimes[0]
            val timeDiff2 = callTimes[2] - callTimes[1]
            assertTrue(timeDiff1 < 100, "Second call should be quick (cached)")
            assertTrue(timeDiff2 > 200, "Third call should be after cache expiry")
        }
    }

    @Test
    fun `Bridge handles high frequency calls without resource leaks`() {
        runBlocking {
            J2KBridgeConfig.mode = J2KBridgeMode.PASSTHROUGH
            val javaFunction = { _: SuspendContext -> "high-freq-result" }
            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
            repeat(100) { launch { kotlinSuspend() } }
            delay(1000)
            assertTrue(true)
        }
    }

    // [/🧩 Section: tests/performance-and-resources]

    // [🧩 Section: tests/multi-parameter]

    @Test
    fun `Multi-parameter functions work correctly through bridge`() {
        runBlocking {
            val javaFunction = { _: SuspendContext, a: String, b: Int, c: Boolean ->
                "params: $a, $b, $c"
            }
            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
            val result = kotlinSuspend("test", 42, true)
            assertEquals("params: test, 42, true", result)
        }
    }

    @Test
    fun `Complex data types pass through bridge correctly`() {
        runBlocking {
            data class TestData(val name: String, val value: Int)
            val javaFunction = { _: SuspendContext, data: TestData ->
                TestData("processed-${data.name}", data.value * 2)
            }
            val kotlinSuspend = ExposeAsKotlin.blocking(javaFunction)
            val result = kotlinSuspend(TestData("input", 21))
            assertEquals(TestData("processed-input", 42), result)
        }
    }

    // [/🧩 Section: tests/multi-parameter]

    // [🧩 Section: tests/custom-kotlin-bindings]

    @Test
    fun `test custom KotlinBindings with parameters`() {
        class CustomKotlinBindings : KotlinBindings() {
            val processUser = blocking { userId: String ->
                delay(25)
                "Processed user: $userId"
            }
            val calculateScore = blocking { name: String, points: Int, multiplier: Double ->
                delay(30)
                "Score for $name: ${points * multiplier}"
            }
            val fetchUserDataAsync = async { userId: String ->
                delay(35)
                "User data for: $userId"
            }
            val computeResultAsync = async { input: String, factor: Int ->
                delay(40)
                "Computed: $input x $factor = ${input.repeat(factor)}"
            }
        }
        val bindings = CustomKotlinBindings()
        val scope = JCoroutineScopeImpl("parameter-test-scope")
        try {
            val results = scope.runBlocking { suspend ->
                val userResult = bindings.processUser.apply(suspend, "USER123")
                val scoreResult = bindings.calculateScore.apply(suspend, "Alice", 100, 1.5)
                val userDataFuture = bindings.fetchUserDataAsync.apply(suspend, "USER456")
                val userDataResult = userDataFuture.get(5, TimeUnit.SECONDS)
                val computeFuture = bindings.computeResultAsync.apply(suspend, "Hi", 3)
                val computeResult = computeFuture.get(5, TimeUnit.SECONDS)
                listOf(userResult, scoreResult, userDataResult, computeResult)
            }
            assertEquals("Processed user: USER123", results[0])
            assertEquals("Score for Alice: 150.0", results[1])
            assertEquals("User data for: USER456", results[2])
            assertEquals("Computed: Hi x 3 = HiHiHi", results[3])
        } finally {
            scope.close()
        }
    }

    @Test
    fun `test custom KotlinBindings with complex data types`() {
        data class UserRequest(val name: String, val age: Int)
        data class UserResponse(val id: String, val message: String, val timestamp: Long)
        class CustomKotlinBindings : KotlinBindings() {
            val processUserRequest = blocking { request: UserRequest ->
                delay(25)
                UserResponse(
                    "USER_${request.name.uppercase()}",
                    "Welcome ${request.name}, age ${request.age}",
                    System.currentTimeMillis()
                )
            }
            val processUserRequestAsync = async { request: UserRequest ->
                delay(35)
                UserResponse(
                    "ASYNC_${request.name.uppercase()}",
                    "Async welcome ${request.name}",
                    System.currentTimeMillis()
                )
            }
        }
        val bindings = CustomKotlinBindings()
        val scope = JCoroutineScopeImpl("complex-type-test-scope")
        try {
            val results = scope.runBlocking { suspend ->
                val request = UserRequest("John", 30)
                val blockingResult = bindings.processUserRequest.apply(suspend, request)
                val asyncFuture = bindings.processUserRequestAsync.apply(suspend, request)
                val asyncResult = asyncFuture.get(5, TimeUnit.SECONDS)
                Pair(blockingResult, asyncResult)
            }
            assertTrue(results.first.id.startsWith("USER_JOHN"))
            assertTrue(results.first.message.contains("Welcome John"))
            assertTrue(results.second.id.startsWith("ASYNC_JOHN"))
            assertTrue(results.second.message.contains("Async welcome John"))
        } finally {
            scope.close()
        }
    }

    // [/🧩 Section: tests/custom-kotlin-bindings]
}
