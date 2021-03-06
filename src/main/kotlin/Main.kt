import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.github.kwhat.jnativehook.GlobalScreen
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.*
import javax.swing.JOptionPane
import kotlin.concurrent.fixedRateTimer
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


const val port = 42020

private object State {
    val scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed) { true }, SnackbarHostState())
    val openSessions = mutableStateListOf<DefaultWebSocketServerSession>()
    var overlayConfig by mutableStateOf(OverlayConfig())

    var overlayStatus by mutableStateOf<OverlayStatus>(OverlayStatus.Stopped)
        private set // Disallow direct setting so forgetting to cancel the timer is impossible

    fun updateOverlayStatus(newOverlayStatus: OverlayStatus) {
        (overlayStatus as? OverlayStatus.Running)?.timer?.cancel()
        overlayStatus = newOverlayStatus
    }
}

val colorNameMap = (object { })::class.java.getResource("colors.txt")!!.readText().lines().associate {
    val (name, colorCode) = it.split("=")
    name to Color(0xFF000000 or colorCode.drop(1).toLong(16))
}

fun main() = try {
    application {
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            hostServer()
            setupHotkey()
            setupTwitchBot()
        }

        Window(
            state = WindowState(size = DpSize(500.dp, 800.dp)),
            title = "MemeScreen",
            onCloseRequest = ::exitApplication,
            icon = painterResource("icon.ico")
        ) {
            MaterialTheme(
                colors = Palette
            ) {
                Scaffold(
                    scaffoldState = State.scaffoldState
                ) {
                    App(
                        scaffoldState = State.scaffoldState,
                        openSessions = State.openSessions,
                        overlayStatus = State.overlayStatus,
                        overlayConfig = State.overlayConfig,
                        onOverlayConfigChange = { overlayConfig ->
                            State.overlayConfig = overlayConfig

                            (State.overlayStatus as? OverlayStatus.Running)?.let {
                                if (State.overlayConfig.updateInterval.isPositive() && State.overlayConfig.updateIntervalReductionOnHotkey.isPositive()) {
                                    it.timer.cancel()
                                    State.updateOverlayStatus(
                                        OverlayStatus.Running.getStatusForCurrentInterval(
                                            currentInterval = State.overlayConfig.updateInterval,
                                            openSessions = State.openSessions,
                                            overlayConfig = State.overlayConfig
                                        )
                                    )
                                }
                            }
                        },
                        onIntervalControlButtonClicked = {
                            when (val currentOverlayStatus = State.overlayStatus) {
                                is OverlayStatus.Running -> {
                                    currentOverlayStatus.timer.cancel()
                                    OverlayStatus.Stopped
                                }
                                is OverlayStatus.Stopped -> {
                                    if (State.overlayConfig.updateInterval.isPositive() && State.overlayConfig.updateIntervalReductionOnHotkey.isPositive()) {
                                        OverlayStatus.Running.getStatusForCurrentInterval(
                                            currentInterval = State.overlayConfig.updateInterval,
                                            openSessions = State.openSessions,
                                            overlayConfig = State.overlayConfig
                                        )
                                    } else {
                                        null
                                    }
                                }
                            }
                                ?.let(State::updateOverlayStatus)
                                ?: run {
                                    coroutineScope.launch {
                                        State.scaffoldState.snackbarHostState.showSnackbar("Interval and interval reduction have to be larger than 0.")
                                    }
                                }
                        }
                    )
                }
            }
        }
    }
} catch (e: Throwable) {
    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE);
    exitProcess(0)
}

data class OverlayConfig(
    val updateInterval: Duration = 60.seconds,
    val updateIntervalReductionOnHotkey: Duration = 5.seconds,
    val widthPercent: Float = 30f,
    val heightPercent: Float = 50f,
    val colorName: String = "black"
)

sealed interface OverlayStatus {
    companion object {
        private val coroutineScope = CoroutineScope(Dispatchers.IO)
    }

    object Stopped : OverlayStatus

    data class Running(
        val startTimestamp: Instant,
        val currentInterval: Duration,
        val timer: Timer
    ) : OverlayStatus {
        companion object {
            fun getStatusForCurrentInterval(
                currentInterval: Duration,
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

private fun hostServer() {
    embeddedServer(CIO, port = port) {
        install(WebSockets)

        routing {
            overlayPage()

            webSocket("/socket") {
                println("Got new connection.")
                State.openSessions.add(this)
                updateOverlay(this, State.overlayConfig)

                try {
                    @Suppress("ControlFlowWithEmptyBody")
                    for (frame in incoming) {
                        // ignore
                    }
                } finally {
                    println("User disconnected.")
                    State.openSessions.remove(this)
                }
            }
        }
    }.start(wait = false)
}

private fun setupHotkey() {
    GlobalScreen.registerNativeHook()
    GlobalScreen.addNativeKeyListener(object : NativeKeyListener {
        override fun nativeKeyPressed(nativeEvent: NativeKeyEvent) {
            val currentOverlayStatus = State.overlayStatus

            if (nativeEvent.modifiers and NativeKeyEvent.CTRL_MASK > 0 && nativeEvent.keyCode == 12 && currentOverlayStatus is OverlayStatus.Running) {
                val nextInterval = currentOverlayStatus.currentInterval - State.overlayConfig.updateIntervalReductionOnHotkey

                if (nextInterval.isPositive()) {
                    State.updateOverlayStatus(
                        OverlayStatus.Running.getStatusForCurrentInterval(
                            currentInterval = nextInterval,
                            openSessions = State.openSessions,
                            overlayConfig = State.overlayConfig
                        )
                    )
                }
            }
        }
    })
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
            overlayConfig.heightPercent,
            (colorNameMap[overlayConfig.colorName] ?: Color.Black) .run {
                "#%02x%02x%02x".format(
                    (red * 255).toInt(),
                    (green * 255).toInt(),
                    (blue * 255).toInt()
                )
            }
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

private fun setupTwitchBot() {
    val chatAccountToken = File("data/bot.token").readText()

    val twitchClient = TwitchClientBuilder.builder()
        .withEnableHelix(true)
        .withEnableChat(true)
        .withChatAccount(OAuth2Credential("twitch", chatAccountToken))
        .build()

    val lastCommandUsagePerUser = mutableMapOf<String, Instant>()

    twitchClient.chat.run {
        connect()
        joinChannel(BotConfig.channel)
        sendMessage(BotConfig.channel, "Bot running.")
    }

    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { messageEvent ->
        if (!messageEvent.message.startsWith("#")) {
            return@onEvent
        }

        val parts = messageEvent.message.trimStart('#').split(" ")
        val command = commands.find { it.name == parts.first() } ?: return@onEvent

        if (BotConfig.onlyMods && CommandPermission.MODERATOR in messageEvent.permissions) {
            twitchClient.chat.sendMessage(BotConfig.channel, "You do not have the required permissions to use this command.")
            return@onEvent
        }

        val lastCommandUsedInstant = lastCommandUsagePerUser.getOrPut(messageEvent.user.name) { Instant.now().minusSeconds(BotConfig.userCooldownSeconds) }

        if (Instant.now().isBefore(lastCommandUsedInstant.plusSeconds(BotConfig.userCooldownSeconds)) && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = java.time.Duration.between(Instant.now(), lastCommandUsedInstant.plusSeconds(BotConfig.userCooldownSeconds)).seconds
            twitchClient.chat.sendMessage(BotConfig.channel, "You are still on cooldown. Please try again in $secondsUntilTimeoutOver seconds.")
            return@onEvent
        }

        val commandHandlerScope = CommandHandlerScope(
            chat = twitchClient.chat,
            openSessions = State.openSessions,
            overlayConfig = State.overlayConfig,
            overlayStatus = State.overlayStatus
        )

        command.handler(commandHandlerScope, parts.drop(1))

        if (commandHandlerScope.overlayConfig != State.overlayConfig) {
            State.overlayConfig = commandHandlerScope.overlayConfig
        }

        if (commandHandlerScope.overlayStatus != State.overlayStatus) {
            State.updateOverlayStatus(commandHandlerScope.overlayStatus)
        }

        if (commandHandlerScope.putUserOnCooldown) {
            lastCommandUsagePerUser[messageEvent.user.name] = Instant.now()
        }
    }
}