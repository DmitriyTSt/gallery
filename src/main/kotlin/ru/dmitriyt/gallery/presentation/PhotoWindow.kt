package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.model.PhotoWindowState
import ru.dmitriyt.gallery.presentation.util.ImageInformation
import ru.dmitriyt.gallery.presentation.util.ImageUtil
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

@Composable
fun PhotoWindow(state: PhotoWindowState.Shown, onClose: () -> Unit, onLeftClick: () -> Unit, onRightClick: () -> Unit) {
    val imageState by ImageState(state.file)
    Window(title = state.name, icon = painterResource(AppResources.appIcon), onCloseRequest = onClose) {
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                ImageStateView(imageState)
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
private fun BoxScope.ImageStateView(imageState: LoadingState<ImageBitmap>) {
    when (imageState) {
        is LoadingState.Error -> Column(modifier = Modifier.align(Alignment.Center)) {
            Image(
                painter = rememberVectorPainter(Icons.Default.Lock),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentDescription = null,
            )
            Text(
                text = imageState.message.orEmpty(),
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center,
            )
        }
        is LoadingState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        is LoadingState.Success -> {
            PhotoView(imageState.data)
        }
    }
}

@Composable
private fun BoxScope.PhotoView(data: ImageBitmap) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Image(
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            )
            .scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollableState { delta ->
                    scale += delta / 500
                    scale = max(scale, 1f)
                    if (scale == 1f) {
                        offsetX = 0f
                        offsetY = 0f
                    }
                    delta
                }
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consumeAllChanges()
                    if (scale != 1f) {
                        offsetX += dragAmount.x * scale
                        offsetY += dragAmount.y * scale
                    }
                }
            },
        bitmap = data,
        contentDescription = null,
    )
}

@Composable
private fun ImageState(file: File): State<LoadingState<ImageBitmap>> {
    return produceState<LoadingState<ImageBitmap>>(initialValue = LoadingState.Loading(), file) {
        value = LoadingState.Loading()
        value = try {
            val image = loadImage(file)

            LoadingState.Success(image)
        } catch (e: Exception) {
            e.printStackTrace()
            LoadingState.Error(file.toString())
        }
    }
}

private suspend fun loadImage(file: File): ImageBitmap {
    return withContext(Dispatchers.IO) {
        val bufferedImage = ImageIO.read(file)
        val imageInformation = ImageInformation.readImageInformation(file)
        val fixedOrientation = ImageUtil.fixImageByExif(
            bufferedImage,
            imageInformation.copy(width = bufferedImage.width, height = bufferedImage.height),
        )
        fixedOrientation.toComposeImageBitmap()
    }
}