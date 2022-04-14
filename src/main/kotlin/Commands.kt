
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.github.twitch4j.chat.TwitchChat
import io.ktor.server.websocket.*
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

data class Command(
    val name: String,
    val handler: CommandHandlerScope.(arguments: List<String>) -> Unit
)

data class CommandHandlerScope(
    val chat: TwitchChat,
    val openSessions: SnapshotStateList<DefaultWebSocketServerSession>,
    var overlayConfig: OverlayConfig,
    var overlayStatus: OverlayStatus
)

val commands = selfReferencing<List<Command>> {
    listOf(
        Command(
            name = "help",
            handler = {
                chat.sendMessage(BotConfig.channel, "Available commands: ${this@selfReferencing().joinToString(", ") { "#${it.name}" }}.")
            }
        ),
        Command(
            name = "reduceinterval",
            handler = {
                when (val currentOverlayStatus = overlayStatus) {
                    is OverlayStatus.Stopped -> chat.sendMessage(BotConfig.channel, "The overlay is not running!")
                    is OverlayStatus.Running -> {
                        val nextInterval = currentOverlayStatus.currentInterval - overlayConfig.updateIntervalReductionOnHotkey

                        if (nextInterval.isPositive()) {
                            overlayStatus = currentOverlayStatus.copy(currentInterval = nextInterval)
                            chat.sendMessage(BotConfig.channel, "Reduced interval to ${(overlayStatus as OverlayStatus.Running).currentInterval}.")
                        } else {
                            chat.sendMessage(BotConfig.channel, "Unable to reduce interval below 0.")
                        }
                    }
                }
            }
        ),
        Command(
            name = "interval",
            handler = { arguments ->
                val firstArgument = arguments.firstOrNull() ?: run {
                    chat.sendMessage(BotConfig.channel, "Current interval duration: ${overlayConfig.updateInterval}.")
                    return@Command
                }

                val newIntervalSeconds = firstArgument.toFloatOrNull()?.takeIf { it > 0 } ?: run {
                    chat.sendMessage(BotConfig.channel, "Invalid duration. Please provide a positive number in seconds.")
                    return@Command
                }

                val newIntervalDuration = (newIntervalSeconds * 1000).roundToInt().milliseconds
                overlayConfig = overlayConfig.copy(updateInterval = newIntervalDuration)
                chat.sendMessage(BotConfig.channel, "Updated interval duration to ${newIntervalDuration}.")

                restartOverlayIfNecessary()
            }
        ),
        Command(
            name = "intervalreduction",
            handler = { arguments ->
                val firstArgument = arguments.firstOrNull() ?: run {
                    chat.sendMessage(BotConfig.channel, "Current interval reduction duration: ${overlayConfig.updateIntervalReductionOnHotkey}.")
                    return@Command
                }

                val newIntervalReductionSeconds = firstArgument.toFloatOrNull()?.takeIf { it > 0 } ?: run {
                    chat.sendMessage(BotConfig.channel, "Invalid duration. Please provide a positive number in seconds.")
                    return@Command
                }

                val newIntervalReductionDuration = (newIntervalReductionSeconds * 1000).roundToInt().milliseconds
                overlayConfig = overlayConfig.copy(updateIntervalReductionOnHotkey = newIntervalReductionDuration)
                chat.sendMessage(BotConfig.channel, "Updated interval reduction duration to ${newIntervalReductionDuration}.")

                restartOverlayIfNecessary()
            }
        ),
        Command(
            name = "width",
            handler = { arguments ->
                val firstArgument = arguments.firstOrNull() ?: run {
                    chat.sendMessage(BotConfig.channel, "Current width: ${overlayConfig.widthPercent.roundToInt()}%.")
                    return@Command
                }

                val newWidth = firstArgument.toFloatOrNull()?.takeIf { it in 0f..100f } ?: run {
                    chat.sendMessage(BotConfig.channel, "Invalid width. Please provide a number between 0 and 100.")
                    return@Command
                }

                overlayConfig = overlayConfig.copy(widthPercent = newWidth)
                chat.sendMessage(BotConfig.channel, "Width set to ${newWidth}%.")

                restartOverlayIfNecessary()
            }
        ),
        Command(
            name = "height",
            handler = { arguments ->
                val firstArgument = arguments.firstOrNull() ?: run {
                    chat.sendMessage(BotConfig.channel, "Current height: ${overlayConfig.heightPercent.roundToInt()}%.")
                    return@Command
                }

                val newHeight = firstArgument.toFloatOrNull()?.takeIf { it in 0f..100f } ?: run {
                    chat.sendMessage(BotConfig.channel, "Invalid height. Please provide a number between 0 and 100.")
                    return@Command
                }

                overlayConfig = overlayConfig.copy(heightPercent = newHeight)
                chat.sendMessage(BotConfig.channel, "Height set to ${newHeight}%.")

                restartOverlayIfNecessary()
            }
        ),
        Command(
            name = "start",
            handler = {
                if (overlayStatus is OverlayStatus.Running) {
                    chat.sendMessage(BotConfig.channel, "Overlay is already running.")
                    return@Command
                }

                overlayStatus = OverlayStatus.Running.getStatusForCurrentInterval(
                    currentInterval = overlayConfig.updateInterval,
                    openSessions = openSessions,
                    overlayConfig = overlayConfig
                )
                chat.sendMessage(BotConfig.channel, "Overlay was started.")
            }
        ),
        Command(
            name = "stop",
            handler = {
                if (overlayStatus is OverlayStatus.Stopped) {
                    chat.sendMessage(BotConfig.channel, "Overlay is already stopped.")
                    return@Command
                }

                (overlayStatus as OverlayStatus.Running).timer.cancel()
                overlayStatus = OverlayStatus.Stopped
                chat.sendMessage(BotConfig.channel, "Overlay was stopped.")
            }
        ),
    )
}

private fun CommandHandlerScope.restartOverlayIfNecessary() {
    (overlayStatus as? OverlayStatus.Running)?.let {
        it.timer.cancel()
        overlayStatus = OverlayStatus.Running.getStatusForCurrentInterval(
            currentInterval = overlayConfig.updateInterval,
            openSessions = openSessions,
            overlayConfig = overlayConfig
        )
    }
}