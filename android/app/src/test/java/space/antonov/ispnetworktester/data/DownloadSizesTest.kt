package space.antonov.ispnetworktester.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadSizesTest {
    @Test
    fun mapsLabelsToDecimalByteCounts() {
        assertEquals(10_000_000L, DownloadSizes.bytesFor("10 MB"))
        assertEquals(50_000_000L, DownloadSizes.bytesFor("50 MB"))
        assertEquals(100_000_000L, DownloadSizes.bytesFor("100 MB"))
        assertEquals(250_000_000L, DownloadSizes.bytesFor("250 MB"))
        assertEquals(500_000_000L, DownloadSizes.bytesFor("500 MB"))
        assertEquals(1_000_000_000L, DownloadSizes.bytesFor("1 GB"))
    }

    @Test
    fun returnsNullForUnknownLabel() {
        assertNull(DownloadSizes.bytesFor("unknown"))
    }
}
