package space.antonov.ispnetworktester.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.job
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class DownloadProgress(
    val bytesRead: Long,
    val totalBytes: Long,
    val elapsedMillis: Long,
    val speedMbps: Double,
)

internal data class DownloadTestResult(
    val bytesRead: Long,
    val elapsedMillis: Long,
    val averageMbps: Double,
)

internal class DownloadTestException(message: String) : IOException(message)

internal object DownloadTester {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @OptIn(InternalCoroutinesApi::class)
    suspend fun download(
        address: String,
        port: String,
        sizeBytes: Long,
        httpClient: OkHttpClient = client,
        progressDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        onProgress: (DownloadProgress) -> Unit,
    ): DownloadTestResult {
        if (sizeBytes <= 0L) {
            throw DownloadTestException("Download size must be greater than zero.")
        }

        val url = try {
            HttpUrl.Builder()
                .scheme("http")
                .host(address)
                .port(port.toInt())
                .addPathSegment("download")
                .addQueryParameter("size", sizeBytes.toString())
                .build()
        } catch (_: IllegalArgumentException) {
            throw DownloadTestException("The server address or port is invalid.")
        }

        val request = Request.Builder().url(url).get().build()
        val call = httpClient.newCall(request)
        val job = currentCoroutineContext().job
        val cancellationHandle = job.invokeOnCompletion(onCancelling = true) {
            if (job.isCancelled) call.cancel()
        }

        try {
            return withContext(Dispatchers.IO) {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw DownloadTestException(
                            "Server responded with HTTP ${response.code}.",
                        )
                    }

                    val body = response.body
                        ?: throw DownloadTestException("The server returned an empty response.")

                    val contentLength = body.contentLength()
                    if (contentLength != -1L && contentLength != sizeBytes) {
                        throw DownloadTestException(
                            "Server announced ${contentLength} bytes; expected ${sizeBytes}.",
                        )
                    }

                    val input = body.byteStream()
                    val buffer = ByteArray(64 * 1024)
                    val startNanos = System.nanoTime()
                    var lastUpdateNanos = startNanos
                    var bytesRead = 0L

                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count == -1) break

                        bytesRead += count
                        val nowNanos = System.nanoTime()
                        if (nowNanos - lastUpdateNanos >= 150_000_000L || bytesRead == sizeBytes) {
                            val elapsedMillis = ((nowNanos - startNanos) / 1_000_000L).coerceAtLeast(1L)
                            val speedMbps = bytesRead * 8.0 / elapsedMillis / 1_000.0
                            withContext(progressDispatcher) {
                                onProgress(
                                    DownloadProgress(
                                        bytesRead = bytesRead,
                                        totalBytes = sizeBytes,
                                        elapsedMillis = elapsedMillis,
                                        speedMbps = speedMbps,
                                    ),
                                )
                            }
                            lastUpdateNanos = nowNanos
                        }
                    }

                    val elapsedMillis = (
                        (System.nanoTime() - startNanos) / 1_000_000L
                    ).coerceAtLeast(1L)

                    if (bytesRead != sizeBytes) {
                        throw DownloadTestException(
                            "Incomplete download: received ${bytesRead} of ${sizeBytes} bytes.",
                        )
                    }

                    DownloadTestResult(
                        bytesRead = bytesRead,
                        elapsedMillis = elapsedMillis,
                        averageMbps = bytesRead * 8.0 / elapsedMillis / 1_000.0,
                    )
                }
            }
        } catch (error: IOException) {
            // OkHttp reports Call.cancel() as an IOException. Preserve coroutine
            // cancellation instead of exposing that expected abort as a failure.
            currentCoroutineContext().ensureActive()
            throw error
        } finally {
            cancellationHandle.dispose()
        }
    }
}
