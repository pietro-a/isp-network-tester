package space.antonov.ispnetworktester.data

import kotlin.math.abs

internal data class PingStatistics(
    val attemptedCount: Int,
    val successfulCount: Int,
    val lostCount: Int,
    val packetLossPercent: Double,
    val minimumMillis: Long?,
    val averageMillis: Double?,
    val maximumMillis: Long?,
    val jitterMillis: Double?,
) {
    companion object {
        fun from(samples: List<Long?>, completedCount: Int): PingStatistics {
            val attemptedSamples = samples.take(completedCount.coerceIn(0, samples.size))
            val successfulSamples = attemptedSamples.filterNotNull()
            val lostCount = attemptedSamples.size - successfulSamples.size
            val jitterSamples = attemptedSamples.zipWithNext().mapNotNull { (first, second) ->
                if (first != null && second != null) abs(second - first).toDouble() else null
            }

            return PingStatistics(
                attemptedCount = attemptedSamples.size,
                successfulCount = successfulSamples.size,
                lostCount = lostCount,
                packetLossPercent = if (attemptedSamples.isEmpty()) {
                    0.0
                } else {
                    lostCount * 100.0 / attemptedSamples.size
                },
                minimumMillis = successfulSamples.minOrNull(),
                averageMillis = successfulSamples.takeIf { it.isNotEmpty() }?.average(),
                maximumMillis = successfulSamples.maxOrNull(),
                jitterMillis = jitterSamples.takeIf { it.isNotEmpty() }?.average(),
            )
        }
    }
}
