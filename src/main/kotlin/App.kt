
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.server.websocket.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
@Preview
fun App(
    openSessions: List<DefaultWebSocketServerSession>,
    overlayConfig: OverlayConfig,
    overlayStatus: OverlayStatus,
    onOverlayConfigChange: (OverlayConfig) -> Unit,
    onIntervalControlButtonClicked: () -> Unit
) {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(Color.LightGray)
                .padding(24.dp)
                .fillMaxSize()
        ) {
            Text(
                style = MaterialTheme.typography.body1,
                text = "Overlay hosted on localhost:$port",
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Text(
                style = MaterialTheme.typography.body1,
                text = "Connected clients: ${openSessions.size}",
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Text(
                style = MaterialTheme.typography.body1,
                text = "Status: ${
                    when (overlayStatus) {
                        is OverlayStatus.Running -> "Running (Current Interval: ${overlayStatus.currentInterval})"
                        is OverlayStatus.Stopped -> "Stopped"
                    }
                }",
                modifier = Modifier.padding(bottom = 24.dp)
            )

            TextField(
                label = {
                    Text("Interval in seconds")
                },
                value = overlayConfig.updateInterval.inWholeSeconds.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let {
                        onOverlayConfigChange(overlayConfig.copy(updateInterval = it.seconds))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            TextField(
                label = {
                    Text("Interval decrease on hotkey press (Ctrl + -) in seconds")
                },
                value = overlayConfig.updateIntervalReductionOnHotkey.inWholeSeconds.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let {
                        onOverlayConfigChange(overlayConfig.copy(updateIntervalReductionOnHotkey = it.seconds))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            )

            Text(
                style = MaterialTheme.typography.caption,
                text = "Width in percent: (${overlayConfig.widthPercent.roundToInt()}%)"
            )

            Slider(
                value = overlayConfig.widthPercent,
                onValueChange = {
                    onOverlayConfigChange(overlayConfig.copy(widthPercent = it))
                },
                steps = 9,
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Text(
                style = MaterialTheme.typography.caption,
                text = "Height in percent: (${overlayConfig.heightPercent.roundToInt()}%)"
            )

            Slider(
                value = overlayConfig.heightPercent,
                onValueChange = {
                    onOverlayConfigChange(overlayConfig.copy(heightPercent = it))
                },
                steps = 9,
                valueRange = 0f..100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    onIntervalControlButtonClicked()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when (overlayStatus) {
                        is OverlayStatus.Running -> "Stop"
                        is OverlayStatus.Stopped -> "Start"
                    }
                )
            }
        }
    }
}