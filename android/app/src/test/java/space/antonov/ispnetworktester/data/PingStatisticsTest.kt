package space.antonov.ispnetworktester.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PingStatisticsTest {
    @Test
    fun calculatesStatisticsWhenAllProbesSucceed() {
        val statistics = PingStatistics.from(
            samples = listOf(10L, 20L, 30L, 40L),
            completedCount = 4,
        )

        assertEquals(4, statistics.attemptedCount)
        assertEquals(4, statistics.successfulCount)
        assertEquals(0, statistics.lostCount)
        assertEquals(0.0, statistics.packetLossPercent, 0.0)
        assertEquals(10L, statistics.minimumMillis)
        assertEquals(25.0, statistics.averageMillis!!, 0.0)
        assertEquals(40L, statistics.maximumMillis)
        assertEquals(10.0, statistics.jitterMillis!!, 0.0)
    }

    @Test
    fun reportsFullPacketLossWhenAllProbesTimeOut() {
        val statistics = PingStatistics.from(
            samples = listOf(null, null, null),
            completedCount = 3,
        )

        assertEquals(3, statistics.attemptedCount)
        assertEquals(0, statistics.successfulCount)
        assertEquals(3, statistics.lostCount)
        assertEquals(100.0, statistics.packetLossPercent, 0.0)
        assertNull(statistics.minimumMillis)
        assertNull(statistics.averageMillis)
        assertNull(statistics.maximumMillis)
        assertNull(statistics.jitterMillis)
    }

    @Test
    fun calculatesFractionalPacketLossWithoutIntegerDivision() {
        val statistics = PingStatistics.from(
            samples = listOf(12L, null, 18L),
            completedCount = 3,
        )

        assertEquals(1, statistics.lostCount)
        assertEquals(33.333333, statistics.packetLossPercent, 0.000001)
        assertEquals(15.0, statistics.averageMillis!!, 0.0)
    }

    @Test
    fun calculatesStatisticsFromOnlyCompletedProbesAfterCancellation() {
        val statistics = PingStatistics.from(
            samples = listOf(10L, null, 30L, null, null),
            completedCount = 3,
        )

        assertEquals(3, statistics.attemptedCount)
        assertEquals(2, statistics.successfulCount)
        assertEquals(1, statistics.lostCount)
        assertEquals(100.0 / 3.0, statistics.packetLossPercent, 0.000001)
        assertEquals(20.0, statistics.averageMillis!!, 0.0)
    }

    @Test
    fun doesNotCalculateJitterAcrossTimedOutProbe() {
        val statistics = PingStatistics.from(
            samples = listOf(10L, null, 30L, 40L),
            completedCount = 4,
        )

        assertEquals(10.0, statistics.jitterMillis!!, 0.0)
    }

    @Test
    fun returnsEmptyStatisticsWhenNoProbesHaveCompleted() {
        val statistics = PingStatistics.from(
            samples = listOf(null, null, null),
            completedCount = 0,
        )

        assertEquals(0, statistics.attemptedCount)
        assertEquals(0, statistics.successfulCount)
        assertEquals(0, statistics.lostCount)
        assertEquals(0.0, statistics.packetLossPercent, 0.0)
        assertNull(statistics.minimumMillis)
        assertNull(statistics.averageMillis)
        assertNull(statistics.maximumMillis)
        assertNull(statistics.jitterMillis)
    }

    @Test
    fun ignoresCompletedCountOutsideSampleRange() {
        val statistics = PingStatistics.from(
            samples = listOf(10L, null),
            completedCount = 10,
        )

        assertEquals(2, statistics.attemptedCount)
        assertEquals(1, statistics.lostCount)
        assertEquals(50.0, statistics.packetLossPercent, 0.0)
    }
}
