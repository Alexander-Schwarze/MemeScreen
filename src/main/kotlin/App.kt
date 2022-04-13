import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.random.Random

val APP_DATA: String = System.getenv("APPDATA")
var rendering = false
@Composable
@Preview
fun App() {
    val grid_x = remember{ mutableStateOf(TextFieldValue("3"))}
    val grid_y = remember{ mutableStateOf(TextFieldValue("2"))}
    val startInterval = remember{ mutableStateOf(TextFieldValue("60000"))}
    val reductionAmount = remember{ mutableStateOf(TextFieldValue("5000"))}
    val inUsage = remember { mutableStateOf(false) }
    var currentTimer = "0"
    var timer = Timer()
    MaterialTheme {
        Column (){
            Row {
                OutlinedTextField(
                    value = grid_x.value,
                    onValueChange = { grid_x.value = it },
                    readOnly = inUsage.value,
                    label = { Text(
                        text = "Amount Columns",
                        fontSize = 15.sp,
                        color = Color.Black
                    ) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 5.dp, end = 5.dp)
                        .height(80.dp),
                    shape = RectangleShape,
                    textStyle = TextStyle(
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            Row {
                OutlinedTextField(
                    value = grid_y.value,
                    onValueChange = { grid_y.value = it },
                    readOnly = inUsage.value,
                    label = { Text(
                        text = "Amount Lines",
                        fontSize = 15.sp,
                        color = Color.Black
                    ) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 5.dp, end = 5.dp)
                        .height(80.dp),
                    shape = RectangleShape,
                    textStyle = TextStyle(
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Row {
                OutlinedTextField(
                    value = startInterval.value,
                    onValueChange = { startInterval.value = it },
                    readOnly = inUsage.value,
                    label = { Text(
                        text = "Start Interval (in milliseconds)",
                        fontSize = 15.sp,
                        color = Color.Black
                    ) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 5.dp, end = 5.dp)
                        .height(80.dp),
                    shape = RectangleShape,
                    textStyle = TextStyle(
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }

            Row {
                OutlinedTextField(
                    value = reductionAmount.value,
                    onValueChange = { reductionAmount.value = it },
                    readOnly = inUsage.value,
                    label = { Text(
                        text = "Interval Reduction Amount (in milliseconds)",
                        fontSize = 15.sp,
                        color = Color.Black
                    ) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, start = 5.dp, end = 5.dp)
                        .height(80.dp),
                    shape = RectangleShape,
                    textStyle = TextStyle(
                        fontSize = 30.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
            Row (
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    enabled = !inUsage.value,
                    onClick = {
                        val startTime = System.currentTimeMillis()
                        inUsage.value = true
                        timer = Timer()
                        timer.schedule( object: TimerTask(){
                            override fun run() {
                                rendering = true
                                renderHTML(
                                    grid_x.value.text,
                                    grid_y.value.text,
                                    startInterval.value.text,
                                    reductionAmount.value.text,
                                    true
                                )
                                currentTimer = ((System.currentTimeMillis() - startTime) / 1000 ).toString()
                            }
                        }, 0L, startInterval.value.text.toLong())
                    },
                    modifier = Modifier
                        .padding(bottom = 15.dp, start = 5.dp, top = 30.dp)
                ){
                    Text(text = "Start Render HTML")
                }
                Text(
                    text = currentTimer,
                    modifier = Modifier
                        .padding(bottom = 15.dp, top = 30.dp, start = 40.dp),
                    fontSize = 30.sp

                )
            }
            Row (
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        inUsage.value = false
                        renderHTML(
                            grid_x.value.text,
                            grid_y.value.text,
                            startInterval.value.text,
                            reductionAmount.value.text,
                            false
                        )
                        timer.cancel()
                    },
                    modifier = Modifier
                        .padding(bottom = 15.dp, start = 5.dp, top = 30.dp)
                ) {
                    Text(text = "Stop Rendering Loop")
                }
            }
        }
    }
}

var currentRenderIteration = 0
var lastRandomIndex = -1

override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    return when (keyCode) {
        KeyEvent.KEYCODE_D -> {
            moveShip(MOVE_LEFT)
            true
        }
        KeyEvent.KEYCODE_F -> {
            moveShip(MOVE_RIGHT)
            true
        }
        KeyEvent.KEYCODE_J -> {
            fireMachineGun()
            true
        }
        KeyEvent.KEYCODE_K -> {
            fireMissile()
            true
        }
        else -> super.onKeyUp(keyCode, event)
    }
}

fun renderHTML(grid_x: String, grid_y: String, startInterval: String, reductionAmount: String, rendering: Boolean){
    val folder = "$APP_DATA/MemeScreen"
    val path = Path.of(folder)
    if(!Files.exists(path)){
        val f = File(APP_DATA, "MemeScreen")
        f.mkdir()
    }
    val gridXNumber = grid_x.toInt()
    val gridYNumber = grid_y.toInt()
    fun createGrid(): String {
        var gridString = "<div class=\"grid-container\">\n"

        val single = "<div></div>\n"
        var bound = gridXNumber * gridYNumber
        if(bound <= 0){
            bound = 1
        }
        var currentRandomField = Random.nextInt(bound)
        //println(lastRandomIndex)
        while(currentRandomField == lastRandomIndex){
            currentRandomField = Random.nextInt(bound)
        }
        lastRandomIndex = currentRandomField
        for(i in 0 until gridYNumber) {
            for(j in 0 until gridXNumber) {
                var tmp = single
                val currentIndex = i * gridXNumber + j
                if (currentIndex == currentRandomField) {
                    tmp = tmp.replace("<div>", "<div style=\"background: black;\"><span style = \"color: white;\">$currentRenderIteration</span>")
                }
                gridString += tmp
            }
        }

        gridString += "</div>"
        return gridString
    }

    fun getColumnString(): String {
        return "auto ".repeat(gridXNumber)
    }

    var htmlCode = "" +
            "<!DOCTYPE html>\n" +
            "<html>\n" +
                "<head>\n"
    if(rendering) {
        htmlCode += "<meta http-equiv=\"refresh\" content=\"0.1\">\n"
    }
    htmlCode += "<style>\n" +
                    ".grid-container{\n" +
                        "display: grid;\n" +
                        "grid-template-columns: " + getColumnString() + ";\n" +
                        "width: 100vw;\n" +
                        "height: 100vh;\n" +
                    "}\n" +
                    "html{\n" +
                        "overflow: hidden;\n" +
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
    // println("Generated HTML code: \n$htmlCode")
    currentRenderIteration++
    println("Finished Rendering $currentRenderIteration")
}