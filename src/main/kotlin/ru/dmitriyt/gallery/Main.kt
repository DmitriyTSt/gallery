import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import ru.dmitriyt.gallery.presentation.DirectorySelectorButton
import ru.dmitriyt.gallery.presentation.Gallery
import java.io.File

@Composable
@Preview
fun App() {
    var directory: File? by remember { mutableStateOf(null) }

    MaterialTheme {
        if (directory == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                DirectorySelectorButton(
                    text = "Выбрать директорию",
                    oldDirectory = directory,
                    modifier = Modifier.align(Alignment.Center)
                ) { selected ->
                    directory = selected
                }
            }
        } else {
            Gallery(directory!!) { selected ->
                directory = selected
            }
        }
    }
}

fun main() = application {
    Window(title = "Галерея", onCloseRequest = ::exitApplication) {
        App()
    }
}
