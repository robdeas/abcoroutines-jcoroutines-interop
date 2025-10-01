// [🧩 File Info]
// path: src/test/kotlin/tech/robd/abcoroutines/kt/ABCoroutinesUtilityTests.kt
// description: Utility and integration tests for ABCoroutines helpers, bindings, and execution contexts.
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import tech.robd.abcoroutines.*
import tech.robd.abcoroutines.kt.interop.bindings.JavaBindings
import tech.robd.abcoroutines.kt.interop.bindings.KotlinBindings
import tech.robd.jcoroutines.SuspendContext
import tech.robd.jcoroutines.functiontypes.BinaryFunction
import tech.robd.jcoroutines.internal.JCoroutineScopeImpl
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds

class ABCoroutinesUtilityTests {

    private lateinit var jScope: JCoroutineScopeImpl

    @BeforeEach
    fun setup() {
        println("=== Setting up ABCoroutines utility test ===")
        jScope = JCoroutineScopeImpl("utility-test-scope")
    }

    @AfterEach
    fun teardown() {
        println("=== Tearing down ABCoroutines utility test ===")
        try {
            jScope.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // [🧩 Region: Editable: tests/jc-utility-functions]
    @Test
    fun `test Jc utility functions`() = runTest {
        var attempts = 0
        val retryResult = retry(
            maxAttempts = 3,
            initialDelay = 10.milliseconds,
            backoffMultiplier = 2.0
        ) {
            attempts++
            if (attempts < 2) throw RuntimeException("Attempt $attempts failed")
            "Success on attempt $attempts"
        }

        assertEquals("Success on attempt 2", retryResult)
        assertEquals(2, attempts)

        val startTime = System.currentTimeMillis()
        val parallelResults = AbcApi.parallelVT(
            { delay(50); "Task A" },
            { delay(40); "Task B" },
            { delay(30); "Task C" }
        )
        val totalTime = System.currentTimeMillis() - startTime
        assertEquals(listOf("Task A", "Task B", "Task C"), parallelResults)
        assertTrue(totalTime < 150)

        val zipResult = AbcApi.zipVT(
            { delay(20); "Hello" },
            { delay(25); 42 }
        ) { greeting, number -> "$greeting #$number" }

        assertEquals("Hello #42", zipResult)
    }
    // [/🧩 Region: Editable: tests/jc-utility-functions]

    // [🧩 Region: Editable: tests/utils-helper-functions]
    @Test
    fun `test Utils helper functions`() = runTest {
        var retryAttempts = 0
        val retryResult = retry(
            maxAttempts = 3,
            initialDelay = 5.milliseconds
        ) {
            retryAttempts++
            if (retryAttempts == 1) throw RuntimeException("First attempt fails")
            "Retry succeeded on attempt $retryAttempts"
        }
        assertEquals("Retry succeeded on attempt 2", retryResult)

        val timeoutResult = withTimeout(100) {
            delay(20)
            "Completed within timeout"
        }
        assertEquals("Completed within timeout", timeoutResult)

        val nullResult = withTimeoutOrNull(20) {
            delay(50)
            "Should not complete"
        }
        assertNull(nullResult)

        val parallelStart = System.currentTimeMillis()
        val parallelResults = parallel(
            { delay(30); 1 },
            { delay(25); 2 },
            { delay(20); 3 }
        )
        val duration = System.currentTimeMillis() - parallelStart
        assertEquals(listOf(1, 2, 3), parallelResults)
        assertTrue(duration < 100)

        val zipResult = zip(
            { delay(15); "Left" },
            { delay(10); "Right" }
        ) { l, r -> "$l+$r" }

        assertEquals("Left+Right", zipResult)
    }
    // [/🧩 Region: Editable: tests/utils-helper-functions]

    // [🧩 Region: Editable: tests/function-converters]
    @Test
    fun `test FunctionConverters between Java and Kotlin`() {
        val kotlinAdd: (Int, Int) -> Int = { a, b -> a + b }
        val javaAdd: BinaryFunction<Int, Int, Int> = kotlinAdd.asJava()
        assertEquals(7, javaAdd.apply(3, 4))

        val javaMultiply = BinaryFunction<Int, Int, Int> { x, y -> x * y }
        val kotlinMultiply: (Int, Int) -> Int = javaMultiply.asKotlin()
        assertEquals(20, kotlinMultiply(4, 5))

        val kotlinUnary: (String) -> String = { "Hello $it" }
        val javaUnary = FunctionConverters.fromKotlin(kotlinUnary)
        assertEquals("Hello World", javaUnary.apply("World"))

        val kotlinNullary: () -> String = { "No parameters" }
        val javaNullary = kotlinNullary.asJava()
        assertEquals("No parameters", javaNullary.get())
    }
    // [/🧩 Region: Editable: tests/function-converters]

    // [🧩 Region: Editable: tests/custom-java-bindings]
    @Test
    fun `test custom JavaBindings implementation`() = runTest {
        class CustomJavaBindings : JavaBindings() {
            val processOrder = blocking { suspend: SuspendContext, orderId: String ->
                suspend.delay(30)
                "Order $orderId processed"
            }
            val calculateTotal = blocking { suspend: SuspendContext, items: List<Int> ->
                suspend.delay(20)
                items.sum()
            }
            val fetchUserAsync = async { suspend: SuspendContext, userId: String ->
                CompletableFuture.supplyAsync {
                    Thread.sleep(25)
                    "User data for $userId"
                }
            }
        }

        val bindings = CustomJavaBindings()
        assertEquals("Order ORD123 processed", bindings.processOrder("ORD123"))
        assertEquals(60, bindings.calculateTotal(listOf(10, 20, 30)))
        assertEquals("User data for USER456", bindings.fetchUserAsync("USER456"))
    }
    // [/🧩 Region: Editable: tests/custom-java-bindings]

    // [🧩 Region: Editable: tests/custom-kotlin-bindings]
    @Test
    fun `test custom KotlinBindings implementation`() {
        class CustomKotlinBindings : KotlinBindings() {
            val validateUser = blocking { userId: String ->
                delay(25)
                userId.startsWith("USER")
            }
            val fetchDataAsync = async { query: String ->
                delay(35)
                "Data for query: $query"
            }
        }

        val bindings = CustomKotlinBindings()

        val scope1 = JCoroutineScopeImpl("binding-test-1")
        try {
            val isValid = scope1.runBlocking { suspend ->
                bindings.validateUser.apply(suspend, "USER789")
            }
            assertTrue(isValid)
        } finally {
            scope1.close()
        }

        val scope2 = JCoroutineScopeImpl("binding-test-2")
        try {
            val data = scope2.runBlocking { suspend ->
                val future = bindings.fetchDataAsync.apply(suspend, "products")
                future.get(5, TimeUnit.SECONDS)
            }
            assertEquals("Data for query: products", data)
        } finally {
            scope2.close()
        }
    }
    // [/🧩 Region: Editable: tests/custom-kotlin-bindings]

    // [🧩 Region: Editable: tests/execution-contexts]
    @Test
    fun `test ABCoroutines execution contexts`() = runTest {
        val results = mutableListOf<String>()
        val threadNames = mutableSetOf<String>()

        val vtJob1 = ABCoroutines.launchVT {
            threadNames.add(Thread.currentThread().name)
            delay(20); results.add("VT-1")
        }
        val vtJob2 = ABCoroutines.launchVT {
            threadNames.add(Thread.currentThread().name)
            delay(25); results.add("VT-2")
        }
        val vtAsync = ABCoroutines.asyncVT {
            threadNames.add(Thread.currentThread().name)
            delay(15); "VT-async"
        }

        vtJob1.join(); vtJob2.join()
        results.add(vtAsync.await())

        assertEquals(3, results.size)
        assertTrue(results.containsAll(listOf("VT-1", "VT-2", "VT-async")))
        assertTrue(threadNames.isNotEmpty())
    }
    // [/🧩 Region: Editable: tests/execution-contexts]

    // [🧩 Region: Editable: tests/runBlockingVT]
    @Test
    fun `test runBlockingVT functionality`() {
        val result = ABCoroutines.runBlockingVT {
            val d1 = async { delay(30); "First" }
            val d2 = async { delay(25); "Second" }
            "${d1.await()} + ${d2.await()}"
        }
        assertEquals("First + Second", result)
    }
    // [/🧩 Region: Editable: tests/runBlockingVT]

    // [🧩 Region: Editable: tests/race-conditions]
    @Test
    fun `test race conditions and resource management`() = runTest {
        val counter = AtomicInteger(0)
        val created = AtomicInteger(0)
        val cleaned = AtomicInteger(0)

        suspend fun resourceIntensiveOperation(id: Int): String {
            created.incrementAndGet()
            try {
                delay(20)
                counter.incrementAndGet()
                return "Resource-$id-processed"
            } finally {
                cleaned.incrementAndGet()
            }
        }

        val result = raceForSuccess(
            { resourceIntensiveOperation(1) },
            { resourceIntensiveOperation(2) },
            { resourceIntensiveOperation(3) }
        )
        assertTrue(result.matches(Regex("Resource-\\d-processed")))

        delay(50)
        assertTrue(created.get() >= 1)
        assertEquals(created.get(), cleaned.get())
    }
    // [/🧩 Region: Editable: tests/race-conditions]

    // [🧩 Region: Editable: tests/complex-workflow]
    @Test
    fun `test complex workflow integration`() = runTest {
        data class WorkItem(val id: String, val data: String)

        suspend fun fetchFromAPI(id: String) = WorkItem(id, "api-data-$id").also { delay(30) }
        suspend fun processItem(item: WorkItem) = item.copy(data = "processed-${item.data}").also { delay(25) }
        suspend fun storeItem(item: WorkItem) = "stored-${item.id}".also { delay(20) }
        suspend fun notifyCompletion(result: String) = "notified-$result".also { delay(15) }

        val workflow1 = run {
            val item = fetchFromAPI("item1")
            val processed = processItem(item)
            val stored = storeItem(processed)
            notifyCompletion(stored)
        }

        val workflow2 = AbcApi.parallelVT(
            { fetchFromAPI("item2") },
            { fetchFromAPI("item3") }
        ).let { items ->
            parallel(
                {
                    val processed = processItem(items[0])
                    val stored = storeItem(processed)
                    notifyCompletion(stored)
                },
                {
                    val processed = processItem(items[1])
                    val stored = storeItem(processed)
                    notifyCompletion(stored)
                }
            )
        }

        assertEquals("notified-stored-item1", workflow1)
        assertEquals(2, workflow2.size)
        assertTrue(workflow2.all { it.startsWith("notified-stored-item") })
    }
    // [/🧩 Region: Editable: tests/complex-workflow]
}
