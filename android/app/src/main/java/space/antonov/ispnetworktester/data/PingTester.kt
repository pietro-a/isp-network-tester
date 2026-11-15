package space.antonov.ispnetworktester.data

import com.marsounjan.icmp4a.Icmp
import com.marsounjan.icmp4a.Icmp4a
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.take

internal object PingTester {
    const val PROBE_COUNT = 10

    private const val PROBE_TIMEOUT_MILLIS = 1_000L
    private const val PROBE_INTERVAL_MILLIS = 1_000L
    private const val PACKET_SIZE_BYTES = 56

    suspend fun run(
        target: String,
        onProbe: (index: Int, latencyMillis: Long?) -> Unit,
    ) {
        Icmp4a().pingInterval(
            host = target,
            count = PROBE_COUNT,
            timeoutMillis = PROBE_TIMEOUT_MILLIS,
            packetSize = PACKET_SIZE_BYTES,
            intervalMillis = PROBE_INTERVAL_MILLIS,
        ).take(PROBE_COUNT).collectIndexed { index, status ->
            val latencyMillis = (status.result as? Icmp.PingResult.Success)?.ms
            onProbe(index, latencyMillis)
        }
    }
}
