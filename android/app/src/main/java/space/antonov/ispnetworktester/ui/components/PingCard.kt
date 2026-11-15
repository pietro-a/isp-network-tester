package space.antonov.ispnetworktester.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import space.antonov.ispnetworktester.data.PingStatistics
import space.antonov.ispnetworktester.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun PingCard(
    running: Boolean,
    completedCount: Int,
    finished: Boolean,
    cancelled: Boolean,
    error: String?,
    samples: List<Long?>,
    target: String,
    onTestClick: () -> Unit,
) {
    val statistics = PingStatistics.from(samples, completedCount)

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
                    text = "PING",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    running -> StatusIndicator(MaterialTheme.colorScheme.primary, "Testing")
                    cancelled -> StatusIndicator(MaterialTheme.colorScheme.outline, "Cancelled")
                    error != null -> StatusIndicator(MaterialTheme.colorScheme.error, "Failed")
                    finished -> StatusIndicator(SuccessGreen, "Completed")
                    else -> StatusIndicator(MaterialTheme.colorScheme.outline, "Not tested")
                }
            }

            Text(
                text = "Target: $target",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                samples.forEachIndexed { index, pingResult ->
                    val isComplete = index < completedCount
                    val backgroundColor = when {
                        !isComplete -> MaterialTheme.colorScheme.surfaceVariant
                        pingResult == null -> MaterialTheme.colorScheme.outline
                        else -> SuccessGreen
                    }
                    val marker = when {
                        !isComplete -> "·"
                        pingResult == null -> "—"
                        else -> pingResult.toString()
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(backgroundColor, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = marker,
                            color = if (isComplete) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Text(
                text = when {
                    running -> "Probe ${completedCount + 1} of ${samples.size}"
                    cancelled -> "Test cancelled after $completedCount probes"
                    error != null -> if (completedCount > 0) {
                        "$completedCount probes completed before the error"
                    } else {
                        "Ping test could not start"
                    }
                    finished -> "${statistics.successfulCount} successful · ${statistics.lostCount} lost · ${statistics.packetLossPercent.asPercentage()} packet loss"
                    else -> "${samples.size} probes per test"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (completedCount > 0 && !running) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PingStatistic("Minimum", statistics.minimumMillis?.let { "$it ms" } ?: "—", Modifier.weight(1f))
                    PingStatistic("Average", statistics.averageMillis?.let { String.format(Locale.US, "%.1f ms", it) } ?: "—", Modifier.weight(1f))
                    PingStatistic("Maximum", statistics.maximumMillis?.let { "$it ms" } ?: "—", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PingStatistic("Packet loss", statistics.packetLossPercent.asPercentage(), Modifier.weight(1f))
                    PingStatistic("Jitter", statistics.jitterMillis?.let { String.format(Locale.US, "%.1f ms", it) } ?: "—", Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            if (!running && completedCount > 0 && statistics.lostCount > 0) {
                Text(
                    text = "Some networks filter or deprioritize ICMP traffic. Packet loss does not necessarily indicate a general connectivity problem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = onTestClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (running) "Cancel test" else "Start ping test")
            }
        }
    }
}

private fun Double.asPercentage(): String = String.format(Locale.US, "%.1f%%", this)

@Composable
private fun PingStatistic(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
