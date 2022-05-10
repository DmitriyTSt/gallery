package ru.dmitriyt.gallery.presentation.photolist

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.dmitriyt.gallery.data.model.MonthDividerInfo
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PhotoListScrollBar(
    modifier: Modifier,
    listState: LazyListState,
    monthDividers: List<MonthDividerInfo>,
    rowSize: Int,
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
//        monthDividers.forEach {
//            println("month ${it.title} ${it.index} ${it.monthRowIndex}")
//        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (true) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

            Box(modifier = Modifier.fillMaxHeight().align(Alignment.TopEnd).padding(vertical = 24.dp)) {
                monthDividers.forEach { item ->
                    val monthItemOffset = item.index * elementHeight
                    if (scrollingHoverMonthIndex == item.index) {
                        Card(modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 28.dp)
                            .offset { IntOffset(0, monthItemOffset.roundToInt() - 10) }
                        ) {
                            Text(
                                modifier = Modifier.padding(4.dp),
                                text = item.title,
                                fontSize = 12.sp,
                            )
                        }
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 15.dp)
                                .size(6.dp)
                                .offset { IntOffset(0, monthItemOffset.roundToInt() - 3) },
                            shape = RoundedCornerShape(3.dp),
                        ) {}
                    } else {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 16.dp)
                                .size(4.dp)
                                .offset { IntOffset(0, monthItemOffset.roundToInt() - 2) },
                            shape = RoundedCornerShape(2.dp),
                        ) {}
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

            LaunchedEffect(firstVisibleElementIndex, elementHeight) {
                if (firstVisibleElementIndex != null) {
                    val firstVisibleElement = getItemIndexByRowIndex(monthDividersState, firstVisibleElementIndex, rowSize)
                    offsetY = firstVisibleElement * elementHeight
                    println("offset scroll = $offsetY row = $firstVisibleElementIndex elem = $firstVisibleElement")
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
                                val newRowIndex = getRowIndexByItemIndex(monthDividersState, newElementIndex, rowSize)
                                println("offset drag = ${offsetY} newRow = $newRowIndex newElem = $newElementIndex")
                                listState.scrollToItem(newRowIndex)
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
    val currentElement = (offsetY / elementHeight).roundToInt()
    return monthDividers.findLast { currentElement >= it.index }?.index
}

private fun getItemIndexByRowIndex(monthDividers: List<MonthDividerInfo>, rowIndex: Int, rowSize: Int): Int {
    if (monthDividers.isEmpty()) {
        return rowIndex * rowSize
    }
    val currentMonth = monthDividers.findLast { rowIndex >= it.monthRowIndex } ?: monthDividers.last()
    return if (rowIndex == currentMonth.monthRowIndex) {
        currentMonth.index
    } else {
        currentMonth.index + 1 + ((rowIndex - 1) - currentMonth.monthRowIndex) * rowSize
    }
}

private fun getRowIndexByItemIndex(monthDividers: List<MonthDividerInfo>, itemIndex: Int, rowSize: Int): Int {
    if (monthDividers.isEmpty()) {
        return itemIndex / rowSize
    }
    val currentMonth = monthDividers.findLast { itemIndex >= it.index } ?: monthDividers.last()
    return if (itemIndex == currentMonth.index) {
        currentMonth.monthRowIndex
    } else {
        currentMonth.monthRowIndex + (itemIndex - currentMonth.index) / 10 + 1
    }
}
