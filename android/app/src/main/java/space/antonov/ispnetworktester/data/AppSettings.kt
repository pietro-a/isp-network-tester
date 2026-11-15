package space.antonov.ispnetworktester.data

data class AppSettings(
    val serverAddress: String = DEFAULT_SERVER_ADDRESS,
    val serverPort: String = DEFAULT_SERVER_PORT,
    val pingAddress: String = DEFAULT_PING_ADDRESS,
    val useServerAddressForPing: Boolean = true,
    val downloadSize: String = DEFAULT_DOWNLOAD_SIZE,
) {
    companion object {
        const val DEFAULT_SERVER_ADDRESS = "test-server.example.net"
        const val DEFAULT_SERVER_PORT = "8080"
        const val DEFAULT_PING_ADDRESS = ""
        const val DEFAULT_DOWNLOAD_SIZE = "100 MB"
    }
}
