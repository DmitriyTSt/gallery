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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
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
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun Gallery(directory: File, windowWidth: Dp, changeDirectory: (File) -> Unit) {
    val viewType = remember { mutableStateOf(GalleryViewType.FOLDERS) }
    val currentDirectory = remember { mutableStateOf(directory) }
    val stateListFiles: MutableState<LoadingState<List<GalleryItem>>> = remember { mutableStateOf(LoadingState.Loading()) }
    val scrollStates = remember { mutableStateOf(mutableMapOf<GalleryViewType, MutableMap<String, LazyListState>>()) }

    LaunchedEffect(directory) {
        currentDirectory.value = directory
    }
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
                is LoadingState.Success -> PhotosList(
                    scrollStates = scrollStates,
                    viewType = viewType.value,
                    key = when (viewType.value) {
                        GalleryViewType.ALL -> directory.toString()
                        GalleryViewType.FOLDERS -> currentDirectory.value.toString()
                    },
                    files = (stateListFiles.value as LoadingState.Success).data,
                    windowWidth = windowWidth,
                ) { newDirectory ->
                    currentDirectory.value = newDirectory
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ObsoleteCoroutinesApi::class)
@Composable
private fun PhotosList(
    scrollStates: MutableState<MutableMap<GalleryViewType, MutableMap<String, LazyListState>>>,
    viewType: GalleryViewType,
    key: String,
    files: List<GalleryItem>,
    windowWidth: Dp,
    onChangeDirectory: (File) -> Unit
) {
    val cellMinSize = 192.dp
    val loadImagesContext = newFixedThreadPoolContext(1, "imagesLoad")
    val state = scrollStates.value[viewType]?.get(key) ?: run {
        val viewTypeState = scrollStates.value[viewType]
        if (viewTypeState == null) {
            scrollStates.value[viewType] = mutableMapOf()
        }
        val newState = rememberLazyListState()
        scrollStates.value[viewType]?.set(key, newState)
        newState
    }
    LazyVerticalGrid(
        state = state,
        cells = GridCells.Adaptive(minSize = cellMinSize)
    ) {
        var lastDividerIndex = -1
        files.forEachIndexed { index, item ->
            val isMonthDivider = item is GalleryItem.MonthDivider
            val maxCellsCount = (windowWidth / cellMinSize).toInt()
            // пустые элементы, так как GridItemSpan не умеет переносить ячейку на всю ширину если в строке уже есть элементы
            if (isMonthDivider) {
                val photoBetweenMonths = index - lastDividerIndex - 1
                repeat((maxCellsCount - (photoBetweenMonths % maxCellsCount)).takeIf { it != maxCellsCount } ?: 0) {
                    item {
                        Box(modifier = Modifier.aspectRatio(1f).fillMaxSize().padding(2.dp))
                    }
                }
                lastDividerIndex = index
            }
            item(
                span = {
                    GridItemSpan(
                        if (!isMonthDivider) {
                            1
                        } else {
                            println("maxCellsCount = $maxCellsCount")
                            maxCellsCount
                        }
                    )
                }
            ) {
                when (item) {
                    is GalleryItem.Directory -> DirectoryItem(item.file, onChangeDirectory)
                    is GalleryItem.MonthDivider -> MonthItem(item.title)
                    is GalleryItem.Photo -> PhotoItem(item.file, loadImagesContext)
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
private fun PhotoItem(photo: File, loadImagesContext: ExecutorCoroutineDispatcher) {
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
                val scaffoldState = rememberScaffoldState()
                val coroutineScope = rememberCoroutineScope()
                Image(
                    bitmap = (imageState as LoadingState.Success).data,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            val attrs = withContext(Dispatchers.IO) {
                                Files.readAttributes(photo.toPath(), BasicFileAttributes::class.java)
                            }
                            val result = scaffoldState
                                .snackbarHostState
                                .showSnackbar("${photo.name}: ${attrs.creationTime()}")
                            when (result) {
                                SnackbarResult.Dismissed -> println("Snackbar dismissed")
                                SnackbarResult.ActionPerformed -> println("Snackbar shown ${photo.name}: ${attrs.creationTime()}")
                            }
                        }
                    }
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

private fun loadFiles(
    viewType: GalleryViewType,
    currentDirectory: File,
    directory: File,
    onLoading: () -> Unit,
    onSuccess: (List<GalleryItem>) -> Unit,
) {
    CoroutineScope(Dispatchers.Main).launch {
        onLoading()
        val listFiles = when (viewType) {
            GalleryViewType.ALL -> getPhotosWithDateSort(directory)
            GalleryViewType.FOLDERS -> currentDirectory.listImages(withDirs = true).map { file ->
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

private fun File.listImages(withDirs: Boolean): List<File> {
    return listFiles()?.toList().orEmpty().filter { (if (withDirs) it.isDirectory else false) || it.isImage() }
}

private fun File.isImage(): Boolean {
    return setOf("jpg", "png", "bmp", "webp", "ico", "gif", "jpeg").contains(this.extension.lowercase())
}

private suspend fun getPhotosWithDateSort(directory: File): List<GalleryItem> = suspendCoroutine { continuation ->
    val fileToAttrs = getAllFilesRecursive(directory)
        .map { it to ImageInformation.getPhotoCreationTime(it) }
        .sortedByDescending { (_, creatingDateTime) ->
            creatingDateTime
        }
    val items = mutableListOf<GalleryItem>()
    val monthFormat = SimpleDateFormat("LLLL yyyy")
    fileToAttrs.forEachIndexed { index, (file, creatingDateTime) ->
        if (index == 0 || !isSameMonths(creatingDateTime, fileToAttrs[index - 1].second)) {
            items.add(
                GalleryItem.MonthDivider(
                    monthFormat
                        .format(creatingDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                )
            )
        }
        items.add(GalleryItem.Photo(file))
    }
    continuation.resume(items)
}

private fun isSameMonths(first: LocalDateTime, second: LocalDateTime): Boolean {
    return first.monthValue == second.monthValue && first.year == second.year
}

private fun getAllFilesRecursive(directory: File): List<File> {
    val dirFiles = directory.listImages(withDirs = true)
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