package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.imgscalr.Scalr
import ru.dmitriyt.gallery.data.model.LoadingState
import java.io.File
import javax.imageio.ImageIO

private val galleryCache = mutableMapOf<String, ImageBitmap>()

@Composable
fun Gallery(directory: File, changeDirectory: (File) -> Unit) {
    Column {
        TopAppBar(
            backgroundColor = Color.White,
            contentColor = Color.Black,
        ) {
            Text(text = "Галерея $directory", modifier = Modifier.padding(16.dp).weight(1f))
            DirectorySelectorButton(
                text = "Изменить",
                oldDirectory = directory,
                modifier = Modifier.padding(end = 16.dp),
                onSelect = changeDirectory,
            )
        }
        val stateListFiles = loadListFiles(directory)
        Box(modifier = Modifier.fillMaxSize()) {
            when (stateListFiles.value) {
                is LoadingState.Error -> Text(text = "Не удалось получить файлы", modifier = Modifier.align(Alignment.Center))
                is LoadingState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is LoadingState.Success -> PhotosList((stateListFiles.value as LoadingState.Success).data)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosList(photos: List<File>) {
    LazyVerticalGrid(
        cells = GridCells.Adaptive(minSize = 192.dp)
    ) {
        photos.forEach { photo ->
            item {
                PhotoItem(photo)
            }
        }
    }
}

@Composable
fun PhotoItem(photo: File) {
    val imageState = loadImageFromFile(photo)
    Box(modifier = Modifier.aspectRatio(1f).fillMaxSize().padding(2.dp)) {
        when (imageState.value) {
            is LoadingState.Error -> Image(
                painter = rememberVectorPainter(Icons.Default.Lock),
                modifier = Modifier.align(Alignment.Center),
                contentDescription = null,
            )
            is LoadingState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is LoadingState.Success -> {
                Image(
                    bitmap = (imageState.value as LoadingState.Success).data,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
fun loadImageFromFile(file: File): State<LoadingState<ImageBitmap>> {
    return produceState<LoadingState<ImageBitmap>>(initialValue = LoadingState.Loading(), file) {
        try {
            val image = galleryCache[file.toString()] ?: run {
                val newImage = withContext(Dispatchers.IO) {
                    val bufferedImage = ImageIO.read(file)
                    val thumbnail = Scalr.resize(
                        bufferedImage,
                        Scalr.Method.SPEED,
                        Scalr.Mode.AUTOMATIC,
                        192 * 2,
                        192 * 2
                    )
                    thumbnail.toComposeImageBitmap()
                }
                galleryCache[file.toString()] = newImage
                newImage
            }

            value = LoadingState.Success(image)
        } catch (e: Exception) {
            value = LoadingState.Error()
        }
    }
}

@Composable
private fun loadListFiles(directory: File): State<LoadingState<List<File>>> {
    return produceState<LoadingState<List<File>>>(initialValue = LoadingState.Loading(), directory) {
        val listFiles = directory.listFiles()?.toList().orEmpty().filter { !it.isDirectory }

        value = LoadingState.Success(listFiles)
    }
}