package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newFixedThreadPoolContext
import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.presentation.items.DirectoryItem
import ru.dmitriyt.gallery.presentation.items.MonthItem
import ru.dmitriyt.gallery.presentation.items.PhotoItem
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ObsoleteCoroutinesApi::class)
@Composable
fun PhotosList(
    scrollStates: MutableState<MutableMap<GalleryViewType, MutableMap<String, LazyListState>>>,
    viewType: GalleryViewType,
    key: String,
    files: List<GalleryItem>,
    windowWidth: Dp,
    onChangeDirectory: (File) -> Unit,
    onPhotoClick: (File, Int, String) -> Unit,
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
                            maxCellsCount
                        }
                    )
                }
            ) {
                when (item) {
                    is GalleryItem.Directory -> DirectoryItem(item.file, onChangeDirectory)
                    is GalleryItem.MonthDivider -> MonthItem(item.title)
                    is GalleryItem.Photo -> PhotoItem(item.file, loadImagesContext) {
                        onPhotoClick(item.file, index, item.file.name)
                    }
                }
            }
        }
    }
}