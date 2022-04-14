
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.ktor.server.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.Instant
import kotlin.concurrent.timer
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toKotlinDuration

@Composable
@Preview
fun App(
    openSessions: List<DefaultWebSocketServerSession>,
    overlayConfig: OverlayConfig,
    overlayStatus: OverlayStatus,
    onOverlayConfigChange: (OverlayConfig) -> Unit,
    onIntervalControlButtonClicked: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    MaterialTheme(
        colors = Palette
    ) {
        val scaffoldState = rememberScaffoldState()

        Scaffold(
            scaffoldState = scaffoldState
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "Overlay hosted on "
                    )

                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "localhost:$port",
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection("http://localhost:$port"), null)
                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "URL copied! Opening browser...",
                                        duration = SnackbarDuration.Short
                                    )

                                    delay(2.seconds)

                                    withContext(Dispatchers.IO) {
                                        Desktop.getDesktop().browse(URI.create("http://localhost:$port"))
                                    }
                                }
                            },
                        textDecoration = TextDecoration.Underline,
                        color = Color(0xff0b5b8e)
                    )
                }

                Text(
                    style = MaterialTheme.typography.body1,
                    text = "Connected clients: ${openSessions.size}",
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                var runningTime by remember { mutableStateOf(0.seconds) }
                val currentOverlayStatus by rememberUpdatedState(overlayStatus)

                DisposableEffect(Unit) {
                    val timer = timer(
                        period = 10.milliseconds.inWholeMilliseconds,
                        daemon = true
                    ) {
                        (currentOverlayStatus as? OverlayStatus.Running)?.let {
                            runningTime = Duration.between(it.startTimestamp, Instant.now()).toKotlinDuration()
                        }
                    }

                    onDispose {
                        timer.cancel()
                    }
                }

                Text(
                    style = MaterialTheme.typography.body1,
                    text = "Status: ${
                        when (overlayStatus) {
                            is OverlayStatus.Running -> "Running for ${runningTime.toString(DurationUnit.SECONDS, 1)} (Current Interval: ${overlayStatus.currentInterval})"
                            is OverlayStatus.Stopped -> "Stopped"
                        }
                    }",
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                TextField(
                    label = {
                        Text("Interval in seconds")
                    },
                    value = (overlayConfig.updateInterval.inWholeMilliseconds.toFloat() / 1000).toString(),
                    onValueChange = { value ->
                        value.toFloatOrNull()?.let {
                            onOverlayConfigChange(overlayConfig.copy(updateInterval = (it * 1000).roundToInt().milliseconds))
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
                    value = (overlayConfig.updateIntervalReductionOnHotkey.inWholeMilliseconds.toFloat() / 1000).toString(),
                    onValueChange = { value ->
                        value.toFloatOrNull()?.let {
                            onOverlayConfigChange(overlayConfig.copy(updateIntervalReductionOnHotkey = (it * 1000).roundToInt().milliseconds))
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

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    style = MaterialTheme.typography.body1,
                    text = "Created by Marc & Alex",
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}