package ru.dmitriyt.gallery.presentation.gallery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.dmitriyt.gallery.data.Settings
import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.LoadingState
import ru.dmitriyt.gallery.data.repository.PhotoListRepository
import ru.dmitriyt.gallery.presentation.base.BaseViewModel
import java.io.File

class GalleryViewModel : BaseViewModel() {

    private val _viewType: MutableStateFlow<GalleryViewType> = MutableStateFlow(Settings.galleryViewType)
    val viewType: StateFlow<GalleryViewType> = _viewType.asStateFlow()

    private val _listFiles: MutableStateFlow<LoadingState<List<GalleryItem>>> = MutableStateFlow(LoadingState.Loading())
    val listFiles: StateFlow<LoadingState<List<GalleryItem>>> = _listFiles.asStateFlow()

    fun loadFiles(viewType: GalleryViewType, currentDirectory: File, directory: File) {
        executeFlow {
            when (viewType) {
                GalleryViewType.ALL -> PhotoListRepository.getPhotosWithDateSort(directory)
                GalleryViewType.FOLDERS -> PhotoListRepository.getPhotoDirectories(currentDirectory)
            }
        }.collectTo(_listFiles)
    }

    fun changeViewType() {
        val newViewType = GalleryViewType.values().let { it[(viewType.value.ordinal + 1) % it.size] }
        _viewType.value = newViewType
        Settings.galleryViewType = newViewType
    }
}