import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

fun renderHTML(){
    
}