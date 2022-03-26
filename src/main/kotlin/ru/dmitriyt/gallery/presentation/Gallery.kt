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
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.imgscalr.Scalr
import ru.dmitriyt.gallery.data.GalleryCacheStorage
import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.presentation.util.ImageInformation
import ru.dmitriyt.gallery.presentation.util.ImageUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun Gallery(directory: File, changeDirectory: (File) -> Unit) {
    val viewType = remember { mutableStateOf(GalleryViewType.FOLDERS) }
    val currentDirectory = remember { mutableStateOf(directory) }
    val stateListFiles: MutableState<LoadingState<List<GalleryItem>>> = remember { mutableStateOf(LoadingState.Loading()) }

    LaunchedEffect(currentDirectory.value, viewType.value) {
        loadFiles(viewType.value, currentDirectory.value, directory, onLoading = {
            stateListFiles.value = LoadingState.Loading()
        }) { items ->
            stateListFiles.value = LoadingState.Success(items)
        }
        println(currentDirectory.value)
    }

    Column {
        TopAppBar(
            backgroundColor = Color.White,
            contentColor = Color.Black,
        ) {
            if (currentDirectory.value != directory && viewType.value == GalleryViewType.FOLDERS) {
                IconButton(onClick = {
                    currentDirectory.value = currentDirectory.value.parentFile
                }) {
                    Icon(painter = rememberVectorPainter(Icons.Default.ArrowBack), contentDescription = null)
                }
            }
            val currentName = when (viewType.value) {
                GalleryViewType.ALL -> directory
                GalleryViewType.FOLDERS -> currentDirectory.value
            }
            Text(text = "Галерея $currentName", modifier = Modifier.padding(16.dp).weight(1f))
            IconButton(onClick = {
                viewType.value = GalleryViewType.values().let { it[(viewType.value.ordinal + 1) % it.size] }
            }) {
                Icon(
                    painter = rememberVectorPainter(
                        when (viewType.value) {
                            GalleryViewType.ALL -> Icons.Default.DateRange
                            GalleryViewType.FOLDERS -> Icons.Default.List
                        }
                    ),
                    contentDescription = null,
                )
            }
            DirectorySelectorButton(
                text = "Изменить",
                oldDirectory = directory,
                modifier = Modifier.padding(end = 16.dp),
                onSelect = changeDirectory,
            )
        }
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
private fun PhotosList(files: List<GalleryItem>, onChangeDirectory: (File) -> Unit) {
    LazyVerticalGrid(
        cells = GridCells.Adaptive(minSize = 192.dp)
    ) {
        files.forEach { item ->
            item(
                span = {
                    GridItemSpan(
                        if (item !is GalleryItem.MonthDivider) {
                            println("grid for item = 1")
                            1
                        } else {
                            println("grid for item ${item.title} = $maxCurrentLineSpan")
                            maxCurrentLineSpan
                        }
                    )
                }
            ) {
                when (item) {
                    is GalleryItem.Directory -> DirectoryItem(item.file, onChangeDirectory)
                    is GalleryItem.MonthDivider -> MonthItem(item.title)
                    is GalleryItem.Photo -> PhotoItem(item.file)
                }
            }
        }
    }
}

@Composable
private fun MonthItem(title: String) {
    Text(text = title, modifier = Modifier.padding(24.dp))
}

@Composable
private fun PhotoItem(photo: File) {
    val imageState = loadImageFromFile(photo)
    Box(modifier = Modifier.aspectRatio(1f).fillMaxSize().padding(2.dp)) {
        when (imageState.value) {
            is LoadingState.Error -> Column(modifier = Modifier.align(Alignment.Center)) {
                Image(
                    painter = rememberVectorPainter(Icons.Default.Lock),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentDescription = null,
                )
                Text(
                    text = (imageState.value as LoadingState.Error).message.orEmpty(),
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center,
                )
            }
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
            val image = GalleryCacheStorage.getFromCache(file.toString()) ?: run {
                val newImage = withContext(Dispatchers.IO) {
                    val bufferedImage = ImageIO.read(file)
                    val imageInformation = ImageInformation.readImageInformation(file)
                    val thumbnail = withContext(Dispatchers.Main) {
                        val resized = Scalr.resize(
                            bufferedImage,
                            Scalr.Method.SPEED,
                            Scalr.Mode.AUTOMATIC,
                            192 * 2,
                            192 * 2
                        )
                        ImageUtil.fixImageByExif(resized, imageInformation.copy(width = resized.width, height = resized.height))
                    }
                    thumbnail.toComposeImageBitmap()
                }
                GalleryCacheStorage.addToCache(file.toString(), newImage)
                newImage
            }

            value = LoadingState.Success(image)
        } catch (e: Exception) {
            e.printStackTrace()
            value = LoadingState.Error(file.toString())
        }
    }
}

private fun loadFiles(
    viewType: GalleryViewType,
    currentDirectory: File,
    directory: File,
    onLoading: () -> Unit,
    onSuccess: (List<GalleryItem>) -> Unit
) {
    CoroutineScope(Dispatchers.Main).launch {
        onLoading()
        val listFiles = when (viewType) {
            GalleryViewType.ALL -> getPhotosWithDateSort(directory)
            GalleryViewType.FOLDERS -> currentDirectory.listFiles()?.toList().orEmpty().map { file ->
                if (file.isDirectory) {
                    GalleryItem.Directory(file)
                } else {
                    GalleryItem.Photo(file)
                }
            }
        }
        onSuccess(listFiles)
    }
}

private suspend fun getPhotosWithDateSort(directory: File): List<GalleryItem> = suspendCoroutine { continuation ->
    val fileToAttrs = getAllFilesRecursive(directory)
        .map { it to Files.readAttributes(it.toPath(), BasicFileAttributes::class.java) }
        .sortedByDescending { (_, attrs) ->
            attrs.creationTime().toMillis()
        }
    val items = mutableListOf<GalleryItem>()
    val monthFormat = SimpleDateFormat("LLLL yyyy")
    fileToAttrs.forEachIndexed { index, (file, attrs) ->
        if (index == 0 || !isSameMonths(attrs, fileToAttrs[index - 1].second)) {
            items.add(
                GalleryItem.MonthDivider(
                    monthFormat
                        .format(attrs.creationTime().toMillis())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                )
            )
        }
        items.add(GalleryItem.Photo(file))
    }
    continuation.resume(items)
}

private fun isSameMonths(first: BasicFileAttributes, second: BasicFileAttributes): Boolean {
    val dateFirst = Calendar.getInstance().apply {
        timeInMillis = first.creationTime().toMillis()
    }
    val dateSecond = Calendar.getInstance().apply {
        timeInMillis = second.creationTime().toMillis()
    }
    return dateFirst.get(Calendar.MONTH) == dateSecond.get(Calendar.MONTH) &&
            dateFirst.get(Calendar.YEAR) == dateSecond.get(Calendar.YEAR)
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