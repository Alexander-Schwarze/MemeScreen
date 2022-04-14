import java.io.File
import java.util.*

object BotConfig {
    private val properties = Properties().apply {
        load(File("tokens/botconfig.properties").inputStream())
    }

    val channel = properties.getProperty("channel")
    val onlyMods = properties.getProperty("onlymods") == "true"
}