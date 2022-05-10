package ru.dmitriyt.gallery.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.GridItemSpan
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import org.jetbrains.skia.FontWeight
import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.presentation.items.DirectoryItem
import ru.dmitriyt.gallery.presentation.items.MonthItem
import ru.dmitriyt.gallery.presentation.items.PhotoItem
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
                        is GalleryItem.Photo -> PhotoItem(item.file, loadImagesContext) {
                            onPhotoClick(item.file, index, item.file.name)
                        }
                    }
                }
            }
        }

        if (viewType == GalleryViewType.ALL) {
            ScrollBar(
                modifier = Modifier.align(Alignment.TopEnd),
                listState = state,
                monthDividers = monthDividers,
            )
        }
    }
}

data class MonthDividerInfo(
    val title: String,
    val index: Int,
    val photoCount: Int,
    val rows: Int,
    val monthRowIndex: Int,
)

@Composable
private fun ScrollBar(
    modifier: Modifier,
    listState: LazyListState,
    monthDividers: List<MonthDividerInfo>,
) {
    var size by remember { mutableStateOf(IntSize(0, 0)) }
    var elementHeight by remember { mutableStateOf(0f) }
    var monthDividersState by remember { mutableStateOf<List<MonthDividerInfo>>(emptyList()) }
    var isUserScrolling by remember { mutableStateOf(false) }
    var scrollingHoverMonthIndex by remember { mutableStateOf<Int?>(null) }
    val scrollCoroutineScope = rememberCoroutineScope()
    var scrollJob: Job? = null

    LaunchedEffect(monthDividers) {
        monthDividersState = monthDividers
        monthDividers.forEach {
            println("month = ${it.title} ${it.index} ${it.photoCount} ${it.rows} ${it.monthRowIndex}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (isUserScrolling) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            Box(modifier = Modifier.fillMaxHeight().align(Alignment.TopEnd).padding(vertical = 28.dp)) {
                monthDividers.forEachIndexed { arrIndex, item ->
                    val monthItemOffset = item.index * elementHeight
                    val paddingEnd = when (arrIndex % 3) {
                        0 -> 0.dp
                        1 -> 128.dp
                        else -> 256.dp
                    }
                    Card(modifier = Modifier
                        .padding(end = paddingEnd)
                        .align(Alignment.TopEnd)
                        .offset { IntOffset(0, monthItemOffset.roundToInt() - 12) }
                    ) {
                        val fontWeight = if (scrollingHoverMonthIndex == item.index) {
                            FontWeight.BOLD
                        } else {
                            FontWeight.NORMAL
                        }
                        Text(
                            modifier = Modifier.padding(4.dp),
                            text = item.title,
                            fontSize = 12.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight(fontWeight)
                        )
                    }
                }
            }
        }

        Box(modifier = modifier.fillMaxHeight().padding(vertical = 4.dp, horizontal = 2.dp).onSizeChanged { boxSize ->
            size = boxSize
            elementHeight = size.height.toFloat() / listState.layoutInfo.totalItemsCount
        }) {
            var offsetY by remember { mutableStateOf(0f) }
            // индекс строки, а не элемента
            val firstVisibleElementIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            elementHeight = size.height.toFloat() / listState.layoutInfo.totalItemsCount

            LaunchedEffect(firstVisibleElementIndex) {
                if (firstVisibleElementIndex != null) {
                    offsetY = firstVisibleElementIndex * elementHeight
                    scrollingHoverMonthIndex = getScrollingMonthIndex(monthDividersState, offsetY, elementHeight)
                }
            }

            Card(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .size(8.dp, 48.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isUserScrolling = true
                            },
                            onDragEnd = {
                                isUserScrolling = false
                            },
                            onDragCancel = {
                                isUserScrolling = false
                            },
                        ) { change, dragAmount ->
                            change.consumeAllChanges()
                            offsetY += dragAmount.y
                            offsetY = min(max(0f, offsetY), size.height.toFloat() - 48f)
                            scrollingHoverMonthIndex = getScrollingMonthIndex(monthDividersState, offsetY, elementHeight)
                            scrollJob?.cancel()
                            scrollJob = scrollCoroutineScope.launch {
                                delay(100)
                                val newElementIndex = (offsetY / elementHeight).roundToInt()
                                listState.scrollToItem(newElementIndex)
                            }
                        }
                    },
                elevation = 3.dp,
            ) {

            }
        }
    }
}

private fun getScrollingMonthIndex(
    monthDividers: List<MonthDividerInfo>,
    offsetY: Float,
    elementHeight: Float,
): Int? {
    // currentElement - строка
    val currentElement = (offsetY / elementHeight).roundToInt()
    return monthDividers.findLast { currentElement >= it.monthRowIndex }?.index
}
