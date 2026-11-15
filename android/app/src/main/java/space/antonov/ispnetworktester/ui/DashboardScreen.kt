package space.antonov.ispnetworktester.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import space.antonov.ispnetworktester.data.AppSettings
import space.antonov.ispnetworktester.data.DownloadSizes
import space.antonov.ispnetworktester.data.DownloadTester
import space.antonov.ispnetworktester.data.PingTester
import space.antonov.ispnetworktester.data.SettingsRepository
import space.antonov.ispnetworktester.data.ServerConnectionTester
import space.antonov.ispnetworktester.ui.components.DownloadCard
import space.antonov.ispnetworktester.ui.components.PingCard
import space.antonov.ispnetworktester.ui.theme.ToolbarIcon
import space.antonov.ispnetworktester.validation.NetworkAddressValidator

@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val settingsRepository = remember(context) { SettingsRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var serverAddress by remember { mutableStateOf(AppSettings.DEFAULT_SERVER_ADDRESS) }
    var serverPort by remember { mutableStateOf(AppSettings.DEFAULT_SERVER_PORT) }
    var pingAddress by remember { mutableStateOf(AppSettings.DEFAULT_PING_ADDRESS) }
    var useServerAddressForPing by remember { mutableStateOf(true) }
    var downloadSize by remember { mutableStateOf(AppSettings.DEFAULT_DOWNLOAD_SIZE) }

    LaunchedEffect(settingsRepository) {
        val saved = settingsRepository.settings.first()
        serverAddress = saved.serverAddress
        serverPort = saved.serverPort
        pingAddress = saved.pingAddress
        useServerAddressForPing = saved.useServerAddressForPing
        downloadSize = saved.downloadSize
    }

    var downloadRunning by remember { mutableStateOf(false) }
    var downloadCompleted by remember { mutableStateOf(false) }
    var downloadCancelled by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var currentSpeed by remember { mutableDoubleStateOf(0.0) }
    var downloadElapsedMillis by remember { mutableLongStateOf(0L) }
    var downloadMessage by remember { mutableStateOf<String?>(null) }
    var downloadSuccess by remember { mutableStateOf<Boolean?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    var pingRunning by remember { mutableStateOf(false) }
    var pingCompletedCount by remember { mutableIntStateOf(0) }
    var pingFinished by remember { mutableStateOf(false) }
    var pingCancelled by remember { mutableStateOf(false) }
    var pingError by remember { mutableStateOf<String?>(null) }
    var pingSamples by remember { mutableStateOf(List<Long?>(PingTester.PROBE_COUNT) { null }) }
    var pingJob by remember { mutableStateOf<Job?>(null) }

    var notice by remember { mutableStateOf<String?>(null) }
    var connectionTestRunning by remember { mutableStateOf(false) }
    var connectionTestMessage by remember { mutableStateOf<String?>(null) }
    var connectionTestSuccess by remember { mutableStateOf<Boolean?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    val saveSettingsAndClose: () -> Unit = {
        coroutineScope.launch {
            val saved = settingsRepository.settings.first()
            val savedServerAddress = saved.serverAddress
                .takeIf(NetworkAddressValidator::isValidHost)
                ?: AppSettings.DEFAULT_SERVER_ADDRESS
            val savedServerPort = saved.serverPort
                .takeIf(NetworkAddressValidator::isValidPort)
                ?: AppSettings.DEFAULT_SERVER_PORT
            val validServerAddress = serverAddress
                .takeIf(NetworkAddressValidator::isValidHost)
                ?: savedServerAddress
            val validServerPort = serverPort
                .takeIf(NetworkAddressValidator::isValidPort)
                ?: savedServerPort
            val savedPingAddress = saved.pingAddress
                .takeIf { it.isEmpty() || NetworkAddressValidator.isValidHost(it) }
                ?: AppSettings.DEFAULT_PING_ADDRESS
            val validPingAddress = if (useServerAddressForPing) {
                savedPingAddress
            } else {
                pingAddress.takeIf(NetworkAddressValidator::isValidHost)
                    ?: savedPingAddress.takeIf(NetworkAddressValidator::isValidHost)
                    ?: validServerAddress
            }
            val validDownloadSize = downloadSize
                .takeIf { DownloadSizes.bytesFor(it) != null }
                ?: saved.downloadSize.takeIf { DownloadSizes.bytesFor(it) != null }
                ?: AppSettings.DEFAULT_DOWNLOAD_SIZE

            val updated = AppSettings(
                serverAddress = validServerAddress,
                serverPort = validServerPort,
                pingAddress = validPingAddress,
                useServerAddressForPing = useServerAddressForPing,
                downloadSize = validDownloadSize,
            )
            settingsRepository.updateSettings(updated)

            serverAddress = updated.serverAddress
            serverPort = updated.serverPort
            pingAddress = updated.pingAddress
            downloadSize = updated.downloadSize
            showSettings = false
        }
    }

    BackHandler(enabled = showSettings || showAbout) {
        when {
            showAbout -> showAbout = false
            showSettings -> saveSettingsAndClose()
        }
    }

    if (showAbout) {
        AboutScreen(onBack = { showAbout = false })
    } else if (showSettings) {
        SettingsScreen(
            serverAddress = serverAddress,
            onServerAddressChange = { value -> serverAddress = value },
            serverPort = serverPort,
            onServerPortChange = { value -> serverPort = value },
            pingAddress = pingAddress,
            onPingAddressChange = { value -> pingAddress = value },
            useServerAddressForPing = useServerAddressForPing,
            onUseServerAddressForPingChange = { value ->
                if (!value && pingAddress.isBlank()) {
                    pingAddress = serverAddress
                }
                useServerAddressForPing = value
            },
            downloadSize = downloadSize,
            onDownloadSizeChange = { value -> downloadSize = value },
            onBack = saveSettingsAndClose,
            connectionTestRunning = connectionTestRunning,
            connectionTestMessage = connectionTestMessage,
            connectionTestSuccess = connectionTestSuccess,
            onTestConnection = {
                connectionTestRunning = true
                connectionTestMessage = null
                connectionTestSuccess = null
                coroutineScope.launch {
                    val result = ServerConnectionTester.test(serverAddress, serverPort)
                    connectionTestMessage = result.message
                    connectionTestSuccess = result.success
                    connectionTestRunning = false
                }
            },
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "ISP Network Tester",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Settings" },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = ToolbarIcon,
                    )
                }
                IconButton(
                    onClick = { showAbout = true },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "About" },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = ToolbarIcon,
                    )
                }
            }

            Text(
                text = "Server: $serverAddress:$serverPort",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            notice?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            DownloadCard(
                running = downloadRunning,
                completed = downloadCompleted,
                cancelled = downloadCancelled,
                progress = downloadProgress,
                speedMbps = currentSpeed,
                elapsedMillis = downloadElapsedMillis,
                sizeLabel = downloadSize,
                message = downloadMessage,
                success = downloadSuccess,
                onTestClick = {
                    if (downloadRunning) {
                        downloadMessage = "Test cancelled."
                        downloadCancelled = true
                        downloadSuccess = null
                        downloadJob?.cancel()
                        notice = null
                    } else {
                        val sizeBytes = DownloadSizes.bytesFor(downloadSize)
                        if (sizeBytes == null) {
                            downloadCompleted = false
                            downloadCancelled = false
                            downloadProgress = 0f
                            currentSpeed = 0.0
                            downloadElapsedMillis = 0L
                            downloadMessage = "Select a valid download size in Settings."
                            downloadSuccess = false
                        } else {
                            notice = null
                            downloadRunning = true
                            downloadCompleted = false
                            downloadCancelled = false
                            downloadProgress = 0f
                            currentSpeed = 0.0
                            downloadElapsedMillis = 0L
                            downloadMessage = null
                            downloadSuccess = null
                            downloadJob = coroutineScope.launch {
                                try {
                                    val result = DownloadTester.download(
                                        address = serverAddress,
                                        port = serverPort,
                                        sizeBytes = sizeBytes,
                                    ) { update ->
                                        downloadProgress = (
                                            update.bytesRead.toFloat() / update.totalBytes
                                        ).coerceIn(0f, 1f)
                                        currentSpeed = update.speedMbps
                                        downloadElapsedMillis = update.elapsedMillis
                                    }
                                    downloadProgress = 1f
                                    currentSpeed = result.averageMbps
                                    downloadElapsedMillis = result.elapsedMillis
                                    downloadCompleted = true
                                    downloadCancelled = false
                                    downloadMessage = "Download of $downloadSize completed successfully."
                                    downloadSuccess = true
                                } catch (_: CancellationException) {
                                    downloadMessage = "Test cancelled."
                                    downloadCancelled = true
                                    downloadSuccess = null
                                } catch (error: Exception) {
                                    currentCoroutineContext().ensureActive()
                                    downloadMessage = error.message
                                        ?.replace(" to /", " to ")
                                        ?: "The download test failed."
                                    downloadSuccess = false
                                } finally {
                                    downloadRunning = false
                                    downloadJob = null
                                }
                            }
                        }
                    }
                },
            )

            PingCard(
                running = pingRunning,
                completedCount = pingCompletedCount,
                finished = pingFinished,
                cancelled = pingCancelled,
                error = pingError,
                samples = pingSamples,
                target = if (useServerAddressForPing) serverAddress else pingAddress,
                onTestClick = {
                    if (pingRunning) {
                        pingCancelled = true
                        pingJob?.cancel()
                    } else {
                        val target = if (useServerAddressForPing) serverAddress else pingAddress
                        notice = null
                        pingSamples = List(PingTester.PROBE_COUNT) { null }
                        pingCompletedCount = 0
                        pingFinished = false
                        pingCancelled = false
                        pingError = null

                        if (target.isBlank()) {
                            pingError = "Enter a ping target in Settings."
                            pingFinished = true
                        } else {
                            pingRunning = true
                            pingJob = coroutineScope.launch {
                                try {
                                    PingTester.run(target) { index, latencyMillis ->
                                        pingSamples = pingSamples.toMutableList().also {
                                            it[index] = latencyMillis
                                        }
                                        pingCompletedCount = index + 1
                                    }
                                    pingFinished = true
                                } catch (_: CancellationException) {
                                    pingCancelled = true
                                } catch (error: Exception) {
                                    currentCoroutineContext().ensureActive()
                                    pingError = error.message ?: "The ping test failed."
                                    pingFinished = true
                                } finally {
                                    pingRunning = false
                                    pingJob = null
                                }
                            }
                        }
                    }
                },
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
