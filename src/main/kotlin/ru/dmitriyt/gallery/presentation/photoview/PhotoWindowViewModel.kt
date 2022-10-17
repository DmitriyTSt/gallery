package ru.dmitriyt.gallery.presentation.photoview

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.dmitriyt.gallery.data.GalleryCacheStorage
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.repository.PhotoRepository
import ru.dmitriyt.gallery.presentation.base.BaseViewModel
import java.io.File

class PhotoWindowViewModel : BaseViewModel() {
    private val _image: MutableStateFlow<LoadingState<ImageBitmap>> = MutableStateFlow(LoadingState.Loading())
    val image: StateFlow<LoadingState<ImageBitmap>> = _image.asStateFlow()

    fun loadImage(file: File) {
        executeFlow(defaultValue = GalleryCacheStorage.getFromFastCache(file.toString())) {
            PhotoRepository.loadImage(file)
        }.collectTo(_image)
    }
}