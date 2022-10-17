package ru.dmitriyt.gallery.presentation.items

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.repository.PhotoRepository
import ru.dmitriyt.gallery.presentation.base.BaseViewModel
import java.io.File

class PhotoItemViewModel : BaseViewModel() {
    private val _image: MutableStateFlow<LoadingState<ImageBitmap>> = MutableStateFlow(LoadingState.Loading())
    val image: StateFlow<LoadingState<ImageBitmap>> = _image.asStateFlow()

    fun loadImage(file: File, loadImagesContext: ExecutorCoroutineDispatcher) {
        executeFlow { PhotoRepository.loadImagePreview(file, loadImagesContext) }.collectTo(_image)
    }
}