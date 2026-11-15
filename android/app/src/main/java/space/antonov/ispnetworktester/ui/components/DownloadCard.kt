package space.antonov.ispnetworktester.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import space.antonov.ispnetworktester.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun DownloadCard(
    running: Boolean,
    completed: Boolean,
    cancelled: Boolean,
    progress: Float,
    speedMbps: Double,
    elapsedMillis: Long,
    sizeLabel: String,
    message: String?,
    success: Boolean?,
    onTestClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "DOWNLOAD SPEED",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusIndicator(
                    color = when {
                        completed -> SuccessGreen
                        cancelled -> MaterialTheme.colorScheme.outline
                        success == false -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    },
                    label = when {
                        running -> "Testing"
                        cancelled -> "Cancelled"
                        completed -> "Completed"
                        success == false -> "Failed"
                        else -> "Not tested"
                    },
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = if (speedMbps > 0.0) {
                        String.format(Locale.US, "%.1f", speedMbps)
                    } else {
                        "—"
                    },
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Mbps",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = if (completed) "Average speed" else "Current speed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = SuccessGreen,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${(progress * 100).toInt()}% complete",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Test size: $sizeLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Elapsed: ${"%.1f".format(Locale.US, elapsedMillis / 1000.0)} s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        success == false -> MaterialTheme.colorScheme.error
                        cancelled -> MaterialTheme.colorScheme.onSurfaceVariant
                        completed -> SuccessGreen
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }

            Button(
                onClick = onTestClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (running) "Cancel test" else "Start download test")
            }
        }
    }
}
