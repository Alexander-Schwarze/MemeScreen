
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


const val port = 42020

fun main() = application {
    val coroutineScope = rememberCoroutineScope()


    val openSessions = rememberSaveable { mutableStateListOf<DefaultWebSocketServerSession>() }
    var overlayConfig by rememberSaveable { mutableStateOf(OverlayConfig()) }

    var overlayStatus by rememberSaveable { mutableStateOf<OverlayStatus>(OverlayStatus.Stopped) }


    LaunchedEffect(Unit) {
        hostServer(openSessions, overlayConfig)

        GlobalScreen.registerNativeHook()
        GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
            override fun nativeKeyPressed(nativeEvent: NativeKeyEvent) {
                val currentOverlayStatus = overlayStatus

                if (nativeEvent.modifiers and NativeKeyEvent.CTRL_MASK > 0 && nativeEvent.keyCode == 12 && currentOverlayStatus is OverlayStatus.Running) {
                    val nextInterval = currentOverlayStatus.currentInterval - overlayConfig.updateIntervalReductionOnHotkey

                    if (nextInterval.isPositive()) {
                        currentOverlayStatus.timer.cancel()
                        overlayStatus = OverlayStatus.Running.getStatusForCurrentInterval(
                            currentInterval = nextInterval,
                            coroutineScope = coroutineScope,
                            openSessions = openSessions,
                            overlayConfig = overlayConfig
                        )
                    }
                }
            }
        })
    }

    // We cannot restart the effect due to the aforementioned bug so this doesn't work
    /*LaunchedEffect(overlayConfig) {
        while (true) {
            if (overlayStatus is OverlayStatus.Running) {
                openSessions.map {
                    launch {
                        updateOverlay(it)
                    }
                }.joinAll()
            }

            delay(overlayConfig.updateInterval)
        }
    }*/

    Window(
        state = WindowState(size = DpSize(450.dp, 800.dp)),
        title = "MemeScreen",
        onCloseRequest = ::exitApplication,
        icon = painterResource("icon.ico")
    ) {
        App(
            openSessions = openSessions,
            overlayStatus = overlayStatus,
            overlayConfig = overlayConfig,
            onOverlayConfigChange = {
                overlayConfig = it

                // Would really prefer not to do it here, but we have to as part of the aforementioned workaround
                (overlayStatus as? OverlayStatus.Running)?.timer?.cancel()
                overlayStatus = OverlayStatus.Stopped
            },
            onIntervalControlButtonClicked = {
                overlayStatus = when (overlayStatus) {
                    is OverlayStatus.Running -> {
                        (overlayStatus as OverlayStatus.Running).timer.cancel()
                        OverlayStatus.Stopped
                    }
                    is OverlayStatus.Stopped -> {
                        OverlayStatus.Running.getStatusForCurrentInterval(
                            currentInterval = overlayConfig.updateInterval,
                            coroutineScope = coroutineScope,
                            openSessions = openSessions,
                            overlayConfig = overlayConfig
                        )
                    }
                }
            }
        )
    }
}

data class OverlayConfig(
    val updateInterval: Duration = 60.seconds,
    val updateIntervalReductionOnHotkey: Duration = 5.seconds,
    val widthPercent: Float = 70f,
    val heightPercent: Float = 70f,
)

sealed interface OverlayStatus {
    object Stopped : OverlayStatus

    data class Running(
        val startTimestamp: Instant,
        val currentInterval: Duration,
        val timer: Timer
    ) : OverlayStatus {
        companion object {
            fun getStatusForCurrentInterval(
                currentInterval: Duration,
                coroutineScope: CoroutineScope,
                openSessions: List<DefaultWebSocketServerSession>,
                overlayConfig: OverlayConfig
            ) = Running(
                startTimestamp = Instant.now(),
                currentInterval = currentInterval,
                timer = fixedRateTimer(
                    period = currentInterval.inWholeMilliseconds,
                    daemon = true
                ) {
                    coroutineScope.launch {
                        openSessions.forEach {
                            launch {
                                updateOverlay(it, overlayConfig)
                            }
                        }
                    }
                }
            )
        }
    }
}

private fun hostServer(
    openSessions: SnapshotStateList<DefaultWebSocketServerSession>,
    overlayConfig: OverlayConfig
) {
    embeddedServer(CIO, port = port) {
        install(WebSockets)

        routing {
            overlayPage()

            webSocket("/socket") {
                println("Got new connection.")
                openSessions.add(this)
                updateOverlay(this, overlayConfig)

                try {
                    @Suppress("ControlFlowWithEmptyBody")
                    for (frame in incoming) {
                        // ignore
                    }
                } finally {
                    println("User disconnected.")
                    openSessions.remove(this)
                }
            }
        }
    }.start(wait = false)
}

private suspend fun updateOverlay(
    session: DefaultWebSocketServerSession,
    overlayConfig: OverlayConfig
) {
    session.send(
        listOf(
            Random.nextInt(0..(100 - overlayConfig.widthPercent.roundToInt())),
            Random.nextInt(0..(100 - overlayConfig.heightPercent.roundToInt())),
            overlayConfig.widthPercent,
            overlayConfig.heightPercent
        ).joinToString(",")
    )
}

/*suspend fun updateOverlays(
    openSessions: SnapshotStateList<DefaultWebSocketServerSession>,
    overlayConfig: OverlayConfig
) {
    while (true) {
        openSessions.forEach {
            it.send(listOf(Random.nextInt(0..50), Random.nextInt(0..50), 50, 50).joinToString(","))
        }

        println("current interval: ${overlayConfig.updateInterval}")

        delay(overlayConfig.updateInterval)
        println("delay over")
    }
}*/