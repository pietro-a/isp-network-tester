package space.antonov.ispnetworktester.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

internal data class ConnectionTestResult(
    val success: Boolean,
    val message: String,
)

internal object ServerConnectionTester {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .callTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun test(address: String, port: String): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                val url = HttpUrl.Builder()
                    .scheme("http")
                    .host(address)
                    .port(port.toInt())
                    .addPathSegment("healthz")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 204) {
                        ConnectionTestResult(
                            success = true,
                            message = "Connection successful (HTTP 204).",
                        )
                    } else {
                        ConnectionTestResult(
                            success = false,
                            message = "Server responded with HTTP ${response.code}; expected 204.",
                        )
                    }
                }
            } catch (_: UnknownHostException) {
                ConnectionTestResult(false, "Could not resolve the server address.")
            } catch (_: SocketTimeoutException) {
                ConnectionTestResult(false, "Connection timed out. Check the server and network.")
            } catch (_: ConnectException) {
                ConnectionTestResult(false, "Could not connect. Check that the server is running and the port is correct.")
            } catch (error: IOException) {
                ConnectionTestResult(
                    false,
                    "Network error: ${(error.localizedMessage ?: "I/O failure").replace(" to /", " to ")}.",
                )
            } catch (_: IllegalArgumentException) {
                ConnectionTestResult(false, "The server address or port is invalid.")
            }
        }
}
