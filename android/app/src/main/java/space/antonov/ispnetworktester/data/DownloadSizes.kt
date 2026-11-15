package space.antonov.ispnetworktester.data

internal object DownloadSizes {
    val labels = listOf("10 MB", "50 MB", "100 MB", "250 MB", "500 MB", "1 GB")

    fun bytesFor(label: String): Long? = when (label) {
        "10 MB" -> 10_000_000L
        "50 MB" -> 50_000_000L
        "100 MB" -> 100_000_000L
        "250 MB" -> 250_000_000L
        "500 MB" -> 500_000_000L
        "1 GB" -> 1_000_000_000L
        else -> null
    }
}
