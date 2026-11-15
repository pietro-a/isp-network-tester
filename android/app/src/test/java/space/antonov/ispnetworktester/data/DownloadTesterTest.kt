package space.antonov.ispnetworktester.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class DownloadTesterTest {
    @Test
    fun downloadsExpectedBytesAndReportsFinalProgress() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            val payload = "hello".toByteArray()
            server.enqueue(MockResponse().setBody("hello"))

            val progressUpdates = mutableListOf<DownloadProgress>()
            val result = DownloadTester.download(
                address = server.hostName,
                port = server.port.toString(),
                sizeBytes = payload.size.toLong(),
                progressDispatcher = Dispatchers.Unconfined,
            ) { progressUpdates += it }

            assertEquals(payload.size.toLong(), result.bytesRead)
            assertTrue(result.elapsedMillis >= 1L)
            assertTrue(result.averageMbps > 0.0)
            assertEquals(payload.size.toLong(), progressUpdates.last().bytesRead)
            assertEquals(payload.size.toLong(), progressUpdates.last().totalBytes)
            assertEquals("/download?size=5", server.takeRequest().path)
        }
    }

    @Test
    fun rejectsNonSuccessfulHttpResponse() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setResponseCode(503))

            val error = captureDownloadError {
                DownloadTester.download(
                    server.hostName,
                    server.port.toString(),
                    5L,
                    progressDispatcher = Dispatchers.Unconfined,
                ) {}
            }

            assertEquals("Server responded with HTTP 503.", error.message)
        }
    }

    @Test
    fun rejectsUnexpectedContentLengthBeforeReadingBody() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(
                MockResponse()
                    .setBody("hello")
                    .setHeader("Content-Length", "4"),
            )

            val error = captureDownloadError {
                DownloadTester.download(
                    server.hostName,
                    server.port.toString(),
                    5L,
                    progressDispatcher = Dispatchers.Unconfined,
                ) {}
            }

            assertEquals("Server announced 4 bytes; expected 5.", error.message)
        }
    }

    @Test
    fun rejectsIncompleteChunkedDownload() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setChunkedBody("short", 2))

            val error = captureDownloadError {
                DownloadTester.download(
                    server.hostName,
                    server.port.toString(),
                    8L,
                    progressDispatcher = Dispatchers.Unconfined,
                ) {}
            }

            assertEquals("Incomplete download: received 5 of 8 bytes.", error.message)
        }
    }

    @Test
    fun rejectsInvalidAddressPortAndDownloadSizeWithoutRequest() = runBlocking {
        MockWebServer().use { server ->
            server.start()

            val invalidInputs = listOf(
                Triple("http://example.com", server.port.toString(), 5L),
                Triple(server.hostName, "not-a-port", 5L),
                Triple(server.hostName, "0", 5L),
                Triple(server.hostName, "65536", 5L),
                Triple(server.hostName, server.port.toString(), 0L),
                Triple(server.hostName, server.port.toString(), -1L),
            )

            invalidInputs.forEach { (address, port, size) ->
                val error = captureDownloadError {
                    DownloadTester.download(
                        address,
                        port,
                        size,
                        progressDispatcher = Dispatchers.Unconfined,
                    ) {}
                }
                assertTrue(error is DownloadTestException)
            }

            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun cancellationStopsAnActiveRequest() = runBlocking {
        MockWebServer().use { server ->
            server.start()
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

            val job = launch {
                DownloadTester.download(
                    address = server.hostName,
                    port = server.port.toString(),
                    sizeBytes = 5L,
                    httpClient = OkHttpClient(),
                    progressDispatcher = Dispatchers.Unconfined,
                ) {}
            }

            val request = withContext(Dispatchers.IO) {
                server.takeRequest(2, TimeUnit.SECONDS)
            }
            assertNotNull(request)

            job.cancelAndJoin()
            assertTrue(job.isCancelled)
        }
    }

    private suspend fun captureDownloadError(block: suspend () -> Unit): Exception {
        try {
            block()
        } catch (error: Exception) {
            return error
        }
        throw AssertionError("Expected the download to fail.")
    }
}
