package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.dmitriyt.gallery.data.GalleryCacheStorage
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.model.PhotoWindowState
import ru.dmitriyt.gallery.presentation.resources.AppResources
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
        is LoadingState.Loading -> CircularProgressIndicator(modifier = modifier)
        is LoadingState.Success -> {
            PhotoView(
                modifier = modifier,
                image = imageState.data,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PhotoView(modifier: Modifier = Modifier, image: ImageBitmap) {
    var mousePosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var photoSize by remember { mutableStateOf(IntSize(0, 0)) }

    Image(
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            )
            .scrollable(
                orientation = Orientation.Vertical,
                state = rememberScrollableState { delta ->
                    val scaleDivider = photoSize.height / 2
                    val oldScale = scale
                    scale = max(oldScale + delta * scale / scaleDivider, 1f)
                    val centerX = photoSize.width / 2
                    val centerY = photoSize.height / 2
                    val mousePositionXDelta = centerX - mousePosition.x
                    val mousePositionYDelta = centerY - mousePosition.y
                    val scaledMousePositionXDelta = scale * mousePositionXDelta
                    val scaledMousePositionYDelta = scale * mousePositionYDelta
                    offsetX = (scaledMousePositionXDelta - mousePositionXDelta)
                    offsetY = (scaledMousePositionYDelta - mousePositionYDelta)
                    delta
                }
            )
            .onSizeChanged { size ->
                photoSize = size
            }
            .onPointerEvent(PointerEventType.Move) {
                mousePosition = it.changes.first().position
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consumeAllChanges()
                    if (scale != 1f) {
                        offsetX += dragAmount.x * scale
                        offsetY += dragAmount.y * scale
                    }
                }
            },
        bitmap = image,
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
    return GalleryCacheStorage.getFromFastCacheFull(file.toString()) ?: run {
        withContext(Dispatchers.IO) {
            val bufferedImage = ImageIO.read(file)
            val imageInformation = ImageInformation.readImageInformation(file)
            val fixedOrientation = ImageUtil.fixImageByExif(
                bufferedImage,
                imageInformation.copy(width = bufferedImage.width, height = bufferedImage.height),
            )
            val image = fixedOrientation.toComposeImageBitmap()
            GalleryCacheStorage.addToFastCacheFull(file.toString(), image)
            image
        }
    }
}