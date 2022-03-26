package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.imgscalr.Scalr
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.LoadingState
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val galleryCache = mutableMapOf<String, ImageBitmap>()

@Composable
fun Gallery(directory: File, changeDirectory: (File) -> Unit) {
    val viewType = remember { mutableStateOf(GalleryViewType.ALL) }
    val currentDirectory = remember { mutableStateOf(directory) }
    Column {
        TopAppBar(
            backgroundColor = Color.White,
            contentColor = Color.Black,
        ) {
            if (currentDirectory.value != directory) {
                IconButton(onClick = {
                    currentDirectory.value = currentDirectory.value.parentFile
                }) {
                    Icon(painter = rememberVectorPainter(Icons.Default.ArrowBack), contentDescription = null)
                }
            }
            Text(text = "Галерея ${currentDirectory.value}", modifier = Modifier.padding(16.dp).weight(1f))
            DirectorySelectorButton(
                text = "Изменить",
                oldDirectory = directory,
                modifier = Modifier.padding(end = 16.dp),
                onSelect = changeDirectory,
            )
        }
        val stateListFiles = loadListFiles(viewType.value, currentDirectory.value)
        Box(modifier = Modifier.fillMaxSize()) {
            when (stateListFiles.value) {
                is LoadingState.Error -> Text(text = "Не удалось получить файлы", modifier = Modifier.align(Alignment.Center))
                is LoadingState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is LoadingState.Success -> PhotosList((stateListFiles.value as LoadingState.Success).data) { newDirectory ->
                    currentDirectory.value = newDirectory
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotosList(files: List<File>, onChangeDirectory: (File) -> Unit) {
    LazyVerticalGrid(
        cells = GridCells.Adaptive(minSize = 192.dp)
    ) {
        files.forEach { file ->
            item {
                if (file.isDirectory) {
                    DirectoryItem(file, onChangeDirectory)
                } else {
                    PhotoItem(file)
                }
            }
        }
    }
}

@Composable
private fun PhotoItem(photo: File) {
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
private fun DirectoryItem(directory: File, onClick: (File) -> Unit) {
    Box(modifier = Modifier.aspectRatio(1f).fillMaxSize().padding(2.dp).clickable {
        onClick(directory)
    }) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Image(
                painter = rememberVectorPainter(Icons.Default.List),
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentDescription = null,
            )
            Text(
                text = directory.name,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
            )
        }
    }
}

@Composable
private fun loadImageFromFile(file: File): State<LoadingState<ImageBitmap>> {
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
private fun loadListFiles(viewType: GalleryViewType, directory: File): State<LoadingState<List<File>>> {
    return produceState<LoadingState<List<File>>>(initialValue = LoadingState.Loading(), directory) {
        val listFiles = when (viewType) {
            GalleryViewType.ALL -> getPhotosWithDateSort(directory)
            GalleryViewType.FOLDERS -> directory.listFiles()?.toList().orEmpty()
        }

        value = LoadingState.Success(listFiles)
    }
}

private suspend fun getPhotosWithDateSort(directory: File): List<File> = suspendCoroutine { continuation ->
    val result = getAllFilesRecursive(directory)
        .map { it to Files.readAttributes(it.toPath(), BasicFileAttributes::class.java) }
        .sortedByDescending { (_, attrs) ->
            attrs.creationTime().toMillis()
        }
        .map { (file, _) -> file }
    continuation.resume(result)
}

private fun getAllFilesRecursive(directory: File): List<File> {
    val dirFiles = directory.listFiles()?.toList().orEmpty()
    val allFiles = mutableListOf<File>()
    dirFiles.forEach { dirFile ->
        if (dirFile.isDirectory) {
            allFiles.addAll(getAllFilesRecursive(dirFile))
        } else {
            allFiles.add(dirFile)
        }
    }
    return allFiles
}