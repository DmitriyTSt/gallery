package ru.dmitriyt.gallery.presentation.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.imgscalr.Scalr
import ru.dmitriyt.gallery.data.GalleryCacheStorage
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.presentation.util.ImageInformation
import ru.dmitriyt.gallery.presentation.util.ImageUtil
import java.io.File
import javax.imageio.ImageIO

@Composable
fun PhotoItem(photo: File, loadImagesContext: ExecutorCoroutineDispatcher, onImageClick: () -> Unit) {
    val imageState by ImageState(photo, loadImagesContext)
    Box(modifier = Modifier.aspectRatio(1f).fillMaxSize().padding(2.dp)) {
        when (imageState) {
            is LoadingState.Error -> Column(modifier = Modifier.align(Alignment.Center)) {
                Image(
                    painter = rememberVectorPainter(Icons.Default.Lock),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentDescription = null,
                )
                Text(
                    text = (imageState as LoadingState.Error).message.orEmpty(),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
            is LoadingState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is LoadingState.Success -> {
                Image(
                    bitmap = (imageState as LoadingState.Success).data,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        onImageClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun ImageState(file: File, loadImagesContext: ExecutorCoroutineDispatcher): State<LoadingState<ImageBitmap>> {
    return produceState<LoadingState<ImageBitmap>>(initialValue = LoadingState.Loading(), file) {
        value = try {
            val image = loadImage(file, loadImagesContext)

            LoadingState.Success(image)
        } catch (e: Exception) {
            e.printStackTrace()
            LoadingState.Error(file.toString())
        }
    }
}

private suspend fun loadImage(file: File, loadImagesContext: ExecutorCoroutineDispatcher): ImageBitmap {
    return GalleryCacheStorage.getFromFastCache(file.toString()) ?: run {
        val newBufferedImage = GalleryCacheStorage.getFromFileCache(file.toString()) ?: run {
            withContext(Dispatchers.IO) {
                val bufferedImage = ImageIO.read(file)
                val imageInformation = ImageInformation.readImageInformation(file)
                val thumbnail = withContext(loadImagesContext) {
                    val resized = Scalr.resize(
                        bufferedImage,
                        Scalr.Method.SPEED,
                        Scalr.Mode.AUTOMATIC,
                        192 * 2,
                        192 * 2
                    )
                    ImageUtil.fixImageByExif(
                        resized,
                        imageInformation.copy(width = resized.width, height = resized.height),
                    )
                }
                GalleryCacheStorage.addToFileCache(file.toString(), thumbnail)
                thumbnail
            }
        }
        val newImage = newBufferedImage.toComposeImageBitmap()
        GalleryCacheStorage.addToFastCache(file.toString(), newImage)
        newImage
    }
}