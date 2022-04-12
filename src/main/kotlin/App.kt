import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.stream.appendHTML
import kotlinx.html.stream.createHTML
import org.w3c.dom.Node
import org.w3c.dom.html.HTMLDivElement
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.random.Random

val APP_DATA: String = System.getenv("APPDATA")

@Composable
@Preview
fun App() {
    MaterialTheme {
        Row (
            modifier = Modifier
                .fillMaxHeight()
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Bottom)
            ) {
                Button(
                    onClick = {
                        renderHTML()
                    },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 15.dp)
                ){
                    Text(text = "Render HTML")
                }
            }
        }
    }
}

var lastRandomIndex = -1

fun renderHTML(){
    println("Start RENDER")
    val folder = "$APP_DATA/MemeScreen"
    val path = Path.of(folder)
    if(!Files.exists(path)){
        val f = File(APP_DATA, "MemeScreen")
        f.mkdir()
    }
    val grid_x = 3
    val grid_y = 2
    fun createGrid(): String {
        var gridString = "<div class=\"grid-container\">\n"

        val single = "<div></div>\n"
        var i = 0
        var currentRandomField = Random.nextInt(grid_x * grid_y)
        println(lastRandomIndex)
        while(currentRandomField == lastRandomIndex){
            currentRandomField = Random.nextInt(grid_x * grid_y)
        }
        lastRandomIndex = currentRandomField
        while (i < grid_y){
            var j = 0
            while(j < grid_x) {
                var tmp = single
                val currentIndex = i * grid_x + j
                if (currentIndex == currentRandomField) {
                    tmp = tmp.replace("<div>", "<div style=\"background: black;\">")
                }
                gridString += tmp
                j++
            }
            i++
        }

        gridString += "</div>"
        return gridString
    }

    fun getColumnString(): String {
        return "auto ".repeat(grid_x)
    }

    val htmlCode = "" +
            "<!DOCTYPE html>\n" +
            "<html>\n" +
                "<head>\n" +
                "<style>\n" +
                    ".grid-container{\n" +
                        "display: grid;\n" +
                        "grid-template-columns: " + getColumnString() + ";\n" +
                        "width: 100vw;\n" +
                        "height: 100vh;\n" +
                    "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                    createGrid() +
                "</body>\n" +
            "</html>\n"

    File(APP_DATA + "/MemeScreen/MemeScreen.html").printWriter().use { out ->
        out.println(htmlCode)
    }
    println("Generated HTML code: \n$htmlCode")
    println("End RENDER")
}