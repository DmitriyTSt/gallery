package ru.dmitriyt.gallery.presentation.photoview

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.dmitriyt.gallery.data.GalleryCacheStorage
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.model.PhotoWindowState
import ru.dmitriyt.gallery.data.repository.PhotoRepository
import ru.dmitriyt.gallery.presentation.resources.AppResources
import java.io.File

private const val KEY_EVENT_DELAY = 200L

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PhotoWindow(state: PhotoWindowState.Shown, onClose: () -> Unit, onLeftClick: () -> Unit, onRightClick: () -> Unit) {
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)
    val imageState by ImageState(state.file)
    val coroutineScope = rememberCoroutineScope()
    var keyEventJob: Job? = null
    Window(
        title = state.name,
        state = windowState,
        icon = painterResource(AppResources.icons.appIcon),
        onCloseRequest = onClose,
        onKeyEvent = { keyEvent ->
            when (keyEvent.key) {
                Key.DirectionLeft -> {
                    keyEventJob?.cancel()
                    keyEventJob = coroutineScope.launch {
                        delay(KEY_EVENT_DELAY)
                        onLeftClick()
                    }
                    true
                }
                Key.DirectionRight -> {
                    keyEventJob?.cancel()
                    keyEventJob = coroutineScope.launch {
                        delay(KEY_EVENT_DELAY)
                        onRightClick()
                    }
                    true
                }
                else -> false
            }
        }
    ) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ImageStateView(modifier = Modifier.align(Alignment.Center), imageState = imageState)
                FloatingActionButton(
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp),
                    onClick = {
                        onLeftClick()
                    }
                ) {
                    Image(
                        painter = rememberVectorPainter(Icons.Default.ArrowBack),
                        contentDescription = null,
                    )
                }
                FloatingActionButton(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                    onClick = {
                        onRightClick()
                    }
                ) {
                    Image(
                        painter = rememberVectorPainter(Icons.Default.ArrowForward),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageStateView(modifier: Modifier, imageState: LoadingState<ImageBitmap>) {
    when (imageState) {
        is LoadingState.Error -> Column(modifier = modifier) {
            Image(
                painter = rememberVectorPainter(Icons.Default.Warning),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentDescription = null,
            )
            Text(
                text = imageState.message.orEmpty(),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
            )
        }
        is LoadingState.Loading -> CircularProgressIndicator(modifier = modifier)
        is LoadingState.Success -> {
            PhotoView(
                modifier = modifier.fillMaxSize(),
                image = imageState.data,
            )
        }
    }
}


@Composable
private fun ImageState(file: File): State<LoadingState<ImageBitmap>> {
    return produceState<LoadingState<ImageBitmap>>(initialValue = LoadingState.Loading(), file) {
        value = GalleryCacheStorage.getFromFastCache(file.toString())?.let { LoadingState.Success(it) } ?: LoadingState.Loading()
        value = try {
            val image = PhotoRepository.loadImage(file)

            LoadingState.Success(image)
        } catch (e: Exception) {
            e.printStackTrace()
            LoadingState.Error(file.toString())
        }
    }
}
