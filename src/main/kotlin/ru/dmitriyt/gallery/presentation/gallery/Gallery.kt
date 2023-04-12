package ru.dmitriyt.gallery.presentation.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.filled.List
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.model.PhotoWindowState
import ru.dmitriyt.gallery.presentation.DirectorySelectorButton
import ru.dmitriyt.gallery.presentation.base.viewModels
import ru.dmitriyt.gallery.presentation.photolist.PhotoList
import ru.dmitriyt.gallery.presentation.photoview.PhotoWindow
import ru.dmitriyt.gallery.presentation.resources.AppResources
import java.io.File

@Composable
fun Gallery(directory: File, windowWidth: Dp, viewModel: GalleryViewModel = viewModels(), changeDirectory: (File) -> Unit) {
    val viewType by viewModel.viewType.collectAsState()
    val stateListFiles by viewModel.listFiles.collectAsState()
    val currentDirectory = remember { mutableStateOf(directory) }
    val scrollStates = remember { mutableStateOf(mutableMapOf<GalleryViewType, MutableMap<String, LazyGridState>>()) }
    val photoWindow = remember { mutableStateOf<PhotoWindowState>(PhotoWindowState.Hidden) }

    LaunchedEffect(directory) {
        currentDirectory.value = directory
    }
    LaunchedEffect(currentDirectory.value, viewType) {
        viewModel.loadFiles(viewType, currentDirectory.value, directory)
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
                (stateListFiles as? LoadingState.Success)?.data?.let { galleryItems ->
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
                (stateListFiles as? LoadingState.Success)?.data?.let { galleryItems ->
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
            if (currentDirectory.value != directory && viewType == GalleryViewType.FOLDERS) {
                IconButton(onClick = {
                    currentDirectory.value = currentDirectory.value.parentFile
                }) {
                    Icon(painter = rememberVectorPainter(Icons.Default.ArrowBack), contentDescription = null)
                }
            }
            val currentName = when (viewType) {
                GalleryViewType.ALL -> directory
                GalleryViewType.FOLDERS -> currentDirectory.value
            }
            Text(
                text = AppResources.strings().galleryDirectoryTitle(currentName.toString()),
                modifier = Modifier.padding(16.dp).weight(1f)
            )
            IconButton(onClick = {
                viewModel.changeViewType()
            }) {
                Icon(
                    painter = rememberVectorPainter(
                        when (viewType) {
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
            when (stateListFiles) {
                is LoadingState.Error -> Text(text = "Не удалось получить файлы", modifier = Modifier.align(Alignment.Center))
                is LoadingState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is LoadingState.Success -> PhotoList(
                    scrollStates = scrollStates,
                    viewType = viewType,
                    key = when (viewType) {
                        GalleryViewType.ALL -> directory.toString()
                        GalleryViewType.FOLDERS -> currentDirectory.value.toString()
                    },
                    files = (stateListFiles as LoadingState.Success).data,
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