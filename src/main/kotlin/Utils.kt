import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SelfReference<T>(initializer: SelfReference<T>.() -> T)  {
    private val self: T by lazy {
        inner ?: throw IllegalStateException("Do not use the self reference until the object is initialized.")
    }

    private val inner = initializer()
    operator fun invoke(): T = self
}

@Suppress("UNUSED")
fun debugLog(vararg arguments: Any?) = println("[${
    ZonedDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
}, ${
    Throwable().stackTrace[1].let { "${it.className.substringAfterLast('.')}#${it.methodName}@L${it.lineNumber}" }
}] ${
    arguments.joinToString(" | ") { it.toString() }
}")

fun <T : Any> selfReferencing(initializer: SelfReference<T>.() -> T): T = SelfReference(initializer)()