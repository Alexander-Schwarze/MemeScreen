// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import java.util.*
import kotlin.system.exitProcess

fun main() = application {

    Window(
        onCloseRequest = {
            Timer().cancel()
            renderHTML("0", "0", "1", "1", false)
            exitProcess(0)
        },
        title = "MemeScreen",
        state = WindowState(
            position = WindowPosition(Alignment.Center),
            size = WindowSize(400.dp, 700.dp)
        ),
        resizable = false
    ) {
        App()
    }
}
