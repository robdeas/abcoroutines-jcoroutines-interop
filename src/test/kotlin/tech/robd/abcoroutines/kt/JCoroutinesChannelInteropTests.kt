// [🧩 File Info]
// path: src/test/kotlin/tech/robd/abcoroutines/kt/JCoroutinesChannelInteropTests.kt
// description: Channel interop tests between Kotlin coroutines and JCoroutines (bridges, nullables, cancellation, request/response).
// author: Rob Deas
// created: 2025-10-01
// license: Apache 2
// robokeytags_version: 1.0
// robokeytags_specification_url: https://github.com/robokeys/robokeytags/blob/main/SPEC.md
// [/🧩 File Info]

package tech.robd.abcoroutines.kt

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.robd.jcoroutines.ChannelUtils
import tech.robd.jcoroutines.advanced.Dispatcher
import tech.robd.jcoroutines.fn.JCoroutineHandle
import tech.robd.jcoroutines.internal.BaseChannel
import tech.robd.jcoroutines.internal.JCoroutineScopeImpl
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.test.DefaultAsserter.assertTrue

class JCoroutinesChannelInteropTests {

    private lateinit var jScope: JCoroutineScopeImpl
    private lateinit var dispatcher: Dispatcher

    @BeforeEach
    fun setup() {
        jScope = JCoroutineScopeImpl("test-scope")
        dispatcher = Dispatcher.virtualThreads()
    }

    @AfterEach
    fun teardown() {
        try {
            jScope.close()
            dispatcher.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Test
    fun `debug test 1 - basic JCoroutineScope functionality`() {
        try {
            val handle = jScope.async<String> { _ ->
                "test result"
            }
            val result = handle.result().get(5, TimeUnit.SECONDS)
            Assertions.assertEquals("test result", result)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `debug test 2 - basic Channel functionality`() {
        try {
            val channel = ChannelUtils.unlimited<String>()
            val isEmpty = ChannelUtils.isEmpty(channel)
            val isClosed = ChannelUtils.isClosed(channel)
            ChannelUtils.close(channel)
            val isClosedAfter = ChannelUtils.isClosed(channel)
            Assertions.assertTrue(isClosedAfter)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `debug test 3 - InteropChannel functionality`() {
        try {
            val interopChannel = ChannelUtils.unlimitedInterop<String>()
            val isEmpty = ChannelUtils.isEmpty(interopChannel)
            val isClosed = ChannelUtils.isClosed(interopChannel)
            ChannelUtils.close(interopChannel)
            val isClosedAfter = ChannelUtils.isClosed(interopChannel)
            Assertions.assertTrue(isClosedAfter)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `debug test 4 - simple send and receive`() {
        try {
            val channel = ChannelUtils.unlimited<String>()
            val result = jScope.runBlocking<String> { suspend ->
                ChannelUtils.send(suspend, channel, "hello")
                val received = ChannelUtils.receive(suspend, channel)
                ChannelUtils.close(channel)
                received
            }
            Assertions.assertEquals("hello", result)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun `test Kotlin to JCoroutines channel bridge`() = runTest {
        val kotlinChannel = Channel<String>(Channel.Factory.UNLIMITED)
        val jChannel = ChannelUtils.unlimited<String>()
        val results = mutableListOf<String>()

        kotlinChannel.send("msg1")
        kotlinChannel.send("msg2")
        kotlinChannel.send("msg3")
        kotlinChannel.close()

        val bridgeResult = jScope.runBlocking<Int> { suspend ->
            var transferred = 0
            while (transferred < 3) {
                val hasData = !kotlinChannel.isEmpty
                if (hasData) {
                    val item = kotlinChannel.tryReceive().getOrNull()
                    if (item != null) {
                        ChannelUtils.send(suspend, jChannel, item)
                        transferred++
                    } else {
                        break
                    }
                } else {
                    break
                }
            }
            ChannelUtils.close(jChannel)
            transferred
        }

        val consumerHandle = jScope.launch { suspend ->
            try {
                repeat(3) {
                    val item = ChannelUtils.receive(suspend, jChannel)
                    results.add("JC: $item")
                }
            } catch (_: Exception) {
                // swallow for test stability
            }
        }

        consumerHandle.result().get(5, TimeUnit.SECONDS)

        Assertions.assertEquals(3, bridgeResult)
        Assertions.assertEquals(3, results.size)
        Assertions.assertEquals("JC: msg1", results[0])
        Assertions.assertEquals("JC: msg2", results[1])
        Assertions.assertEquals("JC: msg3", results[2])
    }

    @Test
    fun `test JCoroutines to Kotlin channel bridge`() = runTest {
        val jChannel = ChannelUtils.unlimited<String>()
        val kotlinChannel = Channel<String>(Channel.Factory.UNLIMITED)
        val results = mutableListOf<String>()

        val consumerJob = launch {
            for (item in kotlinChannel) {
                results.add("K: $item")
            }
        }

        val producerHandle = jScope.launch { suspend ->
            ChannelUtils.send(suspend, jChannel, "jc-msg1")
            ChannelUtils.send(suspend, jChannel, "jc-msg2")
            ChannelUtils.send(suspend, jChannel, "jc-msg3")
            ChannelUtils.close(jChannel)
        }

        val bridgeJob = launch {
            try {
                var itemCount = 0
                while (itemCount < 3) {
                    val item = suspendCancellableCoroutine<String?> { cont ->
                        jScope.launch { suspend ->
                            var attempts = 0
                            while (attempts < 100) {
                                try {
                                    val received = ChannelUtils.tryReceive(jChannel)
                                    cont.resume(received)
                                    return@launch
                                } catch (e: BaseChannel.ClosedReceiveException) {
                                    cont.resume(null)
                                    return@launch
                                } catch (_: Exception) {
                                    suspend.delay(10)
                                    attempts++
                                }
                            }
                            cont.resumeWithException(TimeoutException("Timeout waiting for channel data"))
                        }
                    }

                    if (item != null) {
                        kotlinChannel.send(item)
                        itemCount++
                    } else {
                        break
                    }
                }
            } finally {
                kotlinChannel.close()
            }
        }

        producerHandle.result().get(10, TimeUnit.SECONDS)
        bridgeJob.join()
        consumerJob.join()

        Assertions.assertEquals(3, results.size)
        Assertions.assertEquals("K: jc-msg1", results[0])
        Assertions.assertEquals("K: jc-msg2", results[1])
        Assertions.assertEquals("K: jc-msg3", results[2])
    }

    @Test
    fun `test InteropChannel with nullable values`() = runTest {
        val interopChannel = ChannelUtils.unlimitedInterop<String>()
        val results = mutableListOf<String>()

        val testHandle = jScope.runBlocking<Int> { suspend ->
            ChannelUtils.sendInterop(suspend, interopChannel, "value1")
            ChannelUtils.sendEmpty(suspend, interopChannel)
            ChannelUtils.sendInterop(suspend, interopChannel, "value2")
            ChannelUtils.sendInterop(suspend, interopChannel, null)
            ChannelUtils.sendInterop(suspend, interopChannel, "value3")

            var count = 0
            repeat(5) {
                val optional = ChannelUtils.receiveInterop(suspend, interopChannel)
                if (optional.isPresent) {
                    results.add("Value: ${optional.get()}")
                } else {
                    results.add("Empty")
                }
                count++
            }

            ChannelUtils.close(interopChannel)
            count
        }

        Assertions.assertEquals(5, testHandle)
        Assertions.assertEquals(5, results.size)
        Assertions.assertEquals("Value: value1", results[0])
        Assertions.assertEquals("Empty", results[1])
        Assertions.assertEquals("Value: value2", results[2])
        Assertions.assertEquals("Empty", results[3])
        Assertions.assertEquals("Value: value3", results[4])
    }

    @Test
    fun `test CompletableFuture to Kotlin coroutine conversion`() = runTest {
        suspend fun <T> JCoroutineHandle<T>.awaitResult(): T {
            return suspendCancellableCoroutine { cont ->
                result().whenComplete { value, throwable ->
                    if (throwable != null) {
                        cont.resumeWithException(throwable)
                    } else {
                        cont.resume(value)
                    }
                }
                cont.invokeOnCancellation { cancel() }
            }
        }

        val handle1 = jScope.async<String> { suspend ->
            suspend.delay(50)
            "async result"
        }
        val result1 = handle1.awaitResult()
        Assertions.assertEquals("async result", result1)

        val jChannel = ChannelUtils.unlimited<String>()
        val handle2 = jScope.async<String> { suspend ->
            ChannelUtils.send(suspend, jChannel, "channel-data")
            val received = ChannelUtils.receive(suspend, jChannel)
            "Processed: $received"
        }
        val result2 = handle2.awaitResult()
        Assertions.assertEquals("Processed: channel-data", result2)
    }

    @Test
    fun `test cancellation across boundaries`() = runTest {
        var processed = 0
        var processingStarted = false

        val processorHandle = jScope.launch { suspend ->
            try {
                processingStarted = true
                repeat(20) { i ->
                    if (suspend.getCancellationToken().isCancelled()) {
                        throw CancellationException("Cancelled during processing")
                    }
                    processed++
                    suspend.delay(25)
                }
            } catch (e: CancellationException) {
                throw e
            }
        }

        var attempts = 0
        while (!processingStarted && attempts < 50) {
            delay(10)
            attempts++
        }

        assertTrue("Processor should have started", processingStarted)

        delay(80)

        val cancelResult = processorHandle.cancel()

        delay(100)

        assertTrue("Processor should be cancelled", processorHandle.result().isCancelled)
        assertTrue("Should have processed some items", processed > 0)
        assertTrue(
            "Should not have processed all items due to cancellation (processed: $processed)",
            processed < 20
        )
    }

    @Test
    fun `test bidirectional request-response pattern`() = runTest {
        val requestChannel = ChannelUtils.unlimited<String>()
        val responseChannel = ChannelUtils.unlimited<String>()
        val responses = mutableListOf<String>()

        val serviceHandle = jScope.launch { suspend ->
            try {
                repeat(3) {
                    val request = ChannelUtils.receive(suspend, requestChannel)
                    val response = "Response to $request"
                    ChannelUtils.send(suspend, responseChannel, response)
                }
            } finally {
                ChannelUtils.close(responseChannel)
            }
        }

        val clientHandle = jScope.launch { suspend ->
            ChannelUtils.send(suspend, requestChannel, "req1")
            ChannelUtils.send(suspend, requestChannel, "req2")
            ChannelUtils.send(suspend, requestChannel, "req3")
            ChannelUtils.close(requestChannel)

            repeat(3) {
                val response = ChannelUtils.receive(suspend, responseChannel)
                responses.add(response)
            }
        }

        serviceHandle.result().get(5, TimeUnit.SECONDS)
        clientHandle.result().get(5, TimeUnit.SECONDS)

        Assertions.assertEquals(3, responses.size)
        Assertions.assertTrue(responses.all { it.startsWith("Response to req") })
    }
}
