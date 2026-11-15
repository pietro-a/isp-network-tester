package space.antonov.ispnetworktester.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import space.antonov.ispnetworktester.data.DownloadSizes
import space.antonov.ispnetworktester.validation.NetworkAddressValidator

@Composable
fun SettingsScreen(
    serverAddress: String,
    onServerAddressChange: (String) -> Unit,
    serverPort: String,
    onServerPortChange: (String) -> Unit,
    pingAddress: String,
    onPingAddressChange: (String) -> Unit,
    useServerAddressForPing: Boolean,
    onUseServerAddressForPingChange: (Boolean) -> Unit,
    downloadSize: String,
    onDownloadSizeChange: (String) -> Unit,
    onBack: () -> Unit,
    connectionTestRunning: Boolean,
    connectionTestMessage: String?,
    connectionTestSuccess: Boolean?,
    onTestConnection: () -> Unit,
) {
    var sizeMenuExpanded by remember { mutableStateOf(false) }
    val downloadSizes = DownloadSizes.labels

    val serverAddressError = when {
        serverAddress.isBlank() -> "Enter a server hostname or IP address."
        !NetworkAddressValidator.isValidHost(serverAddress) ->
            "Enter a valid hostname, IPv4 or IPv6 address."
        else -> null
    }
    val serverPortError = when {
        serverPort.isBlank() -> "Enter a port."
        !NetworkAddressValidator.isValidPort(serverPort) ->
            "Port must be a number from 1 to 65535."
        else -> null
    }
    val pingAddressError = when {
        pingAddress.isBlank() -> "Enter a ping target."
        !NetworkAddressValidator.isValidHost(pingAddress) ->
            "Enter a valid hostname, IPv4 or IPv6 address."
        else -> null
    }

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
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Settings",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("TEST SERVER", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = onServerAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Server address") },
                    placeholder = { Text("hostname or IP address") },
                    singleLine = true,
                    isError = serverAddressError != null,
                    supportingText = serverAddressError?.let { message -> { Text(message) } },
                )
                OutlinedTextField(
                    value = serverPort,
                    onValueChange = onServerPortChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Port") },
                    placeholder = { Text("8080") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = serverPortError != null,
                    supportingText = serverPortError?.let { message -> { Text(message) } },
                )
                Button(
                    onClick = onTestConnection,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = serverAddressError == null && serverPortError == null && !connectionTestRunning,
                ) {
                    Text(if (connectionTestRunning) "Testing connection..." else "Test connection")
                }
                connectionTestMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (connectionTestSuccess == true) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("PING", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = useServerAddressForPing,
                        onCheckedChange = onUseServerAddressForPingChange,
                    )
                    Text("Use server address", style = MaterialTheme.typography.bodyMedium)
                }
                OutlinedTextField(
                    value = if (useServerAddressForPing) serverAddress else pingAddress,
                    onValueChange = onPingAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ping target") },
                    placeholder = { Text("hostname or IP address") },
                    singleLine = true,
                    enabled = !useServerAddressForPing,
                    isError = !useServerAddressForPing && pingAddressError != null,
                    supportingText = if (useServerAddressForPing) {
                        null
                    } else {
                        pingAddressError?.let { message -> { Text(message) } }
                    },
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("DOWNLOAD TEST", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text("Test data size", style = MaterialTheme.typography.bodyMedium)
                Box {
                    OutlinedButton(
                        onClick = { sizeMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(downloadSize, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
                        Text("▼")
                    }
                    DropdownMenu(
                        expanded = sizeMenuExpanded,
                        onDismissRequest = { sizeMenuExpanded = false },
                    ) {
                        downloadSizes.forEach { size ->
                            DropdownMenuItem(
                                text = { Text(size) },
                                onClick = {
                                    onDownloadSizeChange(size)
                                    sizeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
