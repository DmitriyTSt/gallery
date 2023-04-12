package ru.dmitriyt.gallery.presentation.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.presentation.base.itemViewModels

@Composable
fun PhotoItem(
    photo: File,
    loadImagesContext: ExecutorCoroutineDispatcher,
    placeholderVectorPainter: Painter,
    viewModel: PhotoItemViewModel = itemViewModels(photo.absolutePath),
    onImageClick: () -> Unit,
) {
    val imageState by viewModel.image.collectAsState()

    LaunchedEffect(photo) {
        viewModel.loadImage(photo, loadImagesContext)
    }

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
            is LoadingState.Loading -> Image(
                painter = placeholderVectorPainter,
                contentDescription = "",
                modifier = Modifier.align(Alignment.Center)
            )
            is LoadingState.Success -> {
                Box(modifier = Modifier.fillMaxSize().clickable {
                    onImageClick()
                }) {
                    Image(
                        bitmap = (imageState as LoadingState.Success).data,
                        contentScale = ContentScale.Crop,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
