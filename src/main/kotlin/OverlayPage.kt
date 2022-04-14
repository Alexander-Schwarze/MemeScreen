import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Route.overlayPage() {
    get("/") {
        call.respondHtml {
            body {
                style = """
                    padding: 0;
                    margin: 0;
                """.trimIndent()

                style {
                    +"""
                        #overlay {
                            background-color: black;
                            position: absolute;
                        }
                    """.trimIndent()
                }

                div {
                    id = "overlay"
                }

                div {
                    id = "warning"

                    style = """
                        position: absolute;
                        display: flex;
                        width: 100%;
                        height: 100%;
                        color: red;
                        font-family: 'Arial';
                        font-size: 48px;
                        justify-content: center;
                        align-items: center;
                        z-index: 1;
                    """.trimIndent()

                    classes = setOf("hidden")

                    +"Disconnected. Please reload page."
                }

                script {
                    unsafe {
                        raw((object { })::class.java.getResource("OverlayPageLogic.js")!!.readText())
                    }
                }
            }
        }
    }
}