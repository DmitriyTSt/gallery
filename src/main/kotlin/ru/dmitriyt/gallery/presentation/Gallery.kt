package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import ru.dmitriyt.gallery.data.Settings
import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.model.PhotoWindowState
import ru.dmitriyt.gallery.data.repository.PhotoRepository
import ru.dmitriyt.gallery.presentation.items.DirectoryItem
import ru.dmitriyt.gallery.presentation.items.MonthItem
import ru.dmitriyt.gallery.presentation.items.PhotoItem
import ru.dmitriyt.gallery.presentation.photoview.PhotoWindow
import ru.dmitriyt.gallery.presentation.resources.AppResources
import java.io.File

@Composable
fun Gallery(directory: File, windowWidth: Dp, changeDirectory: (File) -> Unit) {
    val viewType = remember { mutableStateOf(Settings.galleryViewType) }
    val currentDirectory = remember { mutableStateOf(directory) }
    val stateListFiles: MutableState<LoadingState<List<GalleryItem>>> = remember { mutableStateOf(LoadingState.Loading()) }
    val scrollStates = remember { mutableStateOf(mutableMapOf<GalleryViewType, MutableMap<String, LazyListState>>()) }
    val photoWindow = remember { mutableStateOf<PhotoWindowState>(PhotoWindowState.Hidden) }

    LaunchedEffect(directory) {
        currentDirectory.value = directory
    }
    LaunchedEffect(currentDirectory.value, viewType.value) {
        loadFiles(viewType.value, currentDirectory.value, directory, onLoading = {
            stateListFiles.value = LoadingState.Loading()
        }) { items ->
            stateListFiles.value = LoadingState.Success(items)
        }
    }

    when (photoWindow.value) {
        PhotoWindowState.Hidden -> Unit
        is PhotoWindowState.Shown -> PhotoWindow(
            state = photoWindow.value as PhotoWindowState.Shown,
            onClose = {
                photoWindow.value = PhotoWindowState.Hidden
            },
            onLeftClick = {
                val state = photoWindow.value as PhotoWindowState.Shown
                var index = state.index
                (stateListFiles.value as? LoadingState.Success)?.data?.let { galleryItems ->
                    do {
                        index--
                        if (index == -1) {
                            index = galleryItems.lastIndex
                        }
                    } while (galleryItems[index] !is GalleryItem.Photo)
                    val item = galleryItems[index] as GalleryItem.Photo
                    photoWindow.value = PhotoWindowState.Shown(
                        index = index,
                        file = item.file,
                        name = item.file.name,
                    )
                }
            },
            onRightClick = {
                val state = photoWindow.value as PhotoWindowState.Shown
                var index = state.index
                (stateListFiles.value as? LoadingState.Success)?.data?.let { galleryItems ->
                    do {
                        index++
                        if (index == galleryItems.size) {
                            index = 0
                        }
                    } while (galleryItems[index] !is GalleryItem.Photo)
                    val item = galleryItems[index] as GalleryItem.Photo
                    photoWindow.value = PhotoWindowState.Shown(
                        index = index,
                        file = item.file,
                        name = item.file.name,
                    )
                }
            }
        )
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
            Text(
                text = AppResources.strings().galleryDirectoryTitle(currentName.toString()),
                modifier = Modifier.padding(16.dp).weight(1f)
            )
            IconButton(onClick = {
                viewType.value = GalleryViewType.values().let { it[(viewType.value.ordinal + 1) % it.size] }
                Settings.galleryViewType = viewType.value
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
                text = AppResources.strings().changeLabel,
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
                    onChangeDirectory = { newDirectory ->
                        currentDirectory.value = newDirectory
                    },
                    onPhotoClick = { file, index, name ->
                        photoWindow.value = PhotoWindowState.Shown(
                            index = index,
                            file = file,
                            name = name,
                        )
                    },
                )
            }
        }
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
            GalleryViewType.ALL -> PhotoRepository.getPhotosWithDateSort(directory)
            GalleryViewType.FOLDERS -> PhotoRepository.getPhotoDirectories(currentDirectory)
        }
        onSuccess(listFiles)
    }
}