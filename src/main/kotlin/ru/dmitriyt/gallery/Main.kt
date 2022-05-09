import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.dmitriyt.gallery.data.Settings
import ru.dmitriyt.gallery.presentation.AppResources
import ru.dmitriyt.gallery.presentation.DirectorySelectorButton
import ru.dmitriyt.gallery.presentation.Gallery
import java.io.File

@Composable
@Preview
fun App(windowWidth: Dp) {
    val directory: MutableState<File?> = remember { mutableStateOf(Settings.directory) }

    MaterialTheme {
        if (directory.value == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                DirectorySelectorButton(
                    text = "Выбрать директорию",
                    oldDirectory = directory.value,
                    modifier = Modifier.align(Alignment.Center)
                ) { selected ->
                    Settings.directory = selected
                    directory.value = selected
                }
            }
        } else {
            Gallery(directory.value!!, windowWidth) { selected ->
                Settings.directory = selected
                directory.value = selected
            }
        }
    }
}

fun main() = application {
    val appIcon = painterResource(AppResources.appIcon)

    Tray(
        icon = appIcon,
        menu = {
            Item("Выйти", onClick = ::exitApplication)
        }
    )

    val state = rememberWindowState()

    Window(title = "Галерея", state = state, onCloseRequest = ::exitApplication, icon = appIcon) {
        App(state.size.width)
    }
}
