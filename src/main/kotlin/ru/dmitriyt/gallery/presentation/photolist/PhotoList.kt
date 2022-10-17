package ru.dmitriyt.gallery.presentation.photolist

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.MonthDividerInfo
import ru.dmitriyt.gallery.presentation.items.DirectoryItem
import ru.dmitriyt.gallery.presentation.items.MonthItem
import ru.dmitriyt.gallery.presentation.items.PhotoItem
import ru.dmitriyt.gallery.presentation.resources.AppResources
import java.io.File
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ObsoleteCoroutinesApi::class)
@Composable
fun PhotoList(
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
    var monthDividers by remember { mutableStateOf<List<MonthDividerInfo>>(emptyList()) }
    val maxCellsCount = (windowWidth / cellMinSize).toInt()

    LaunchedEffect(files, maxCellsCount) {
        CoroutineScope(Dispatchers.Default).launch {
            monthDividers = files
                .mapIndexed { index, galleryItem -> galleryItem to index }
                .filter { (item, _) -> item is GalleryItem.MonthDivider }
                .map { (item, index) -> (item as GalleryItem.MonthDivider) to index }
                .let { monthDividerPairs ->
                    val monthDividersResult = monthDividerPairs.mapIndexed { arrIndex, (item, index) ->
                        val nextMonthIndex = monthDividerPairs.getOrNull(arrIndex + 1)?.second ?: files.size
                        val photoCount = nextMonthIndex.minus(index).minus(1)
                        MonthDividerInfo(
                            title = item.title,
                            index = index,
                            photoCount = photoCount,
                            rows = photoCount.toFloat().div(maxCellsCount).plus(0.5).roundToInt(),
                            monthRowIndex = 0,
                        )
                    }.toMutableList()
                    monthDividersResult.forEachIndexed { index, monthDividerInfo ->
                        monthDividersResult[index] = monthDividerInfo.copy(
                            monthRowIndex = if (index == 0) {
                                0
                            } else {
                                monthDividersResult[index - 1].monthRowIndex + monthDividersResult[index - 1].rows + 1
                            }
                        )
                    }
                    monthDividersResult
                }
        }
    }

    val placeholder = painterResource(AppResources.icons.placeholder)

    Box {
        LazyVerticalGrid(
            state = state,
            cells = GridCells.Adaptive(minSize = cellMinSize)
        ) {
            var lastDividerIndex = -1
            files.forEachIndexed { index, item ->
                val isMonthDivider = item is GalleryItem.MonthDivider
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
                        is GalleryItem.Photo -> PhotoItem(item.file, loadImagesContext, placeholder) {
                            onPhotoClick(item.file, index, item.file.name)
                        }
                    }
                }
            }
        }

        if (viewType == GalleryViewType.ALL) {
            PhotoListScrollBar(
                modifier = Modifier.align(Alignment.TopEnd),
                listState = state,
                monthDividers = monthDividers,
                rowSize = maxCellsCount,
            )
        }
    }
}
