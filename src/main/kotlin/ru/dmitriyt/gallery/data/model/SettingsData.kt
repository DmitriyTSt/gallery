package ru.dmitriyt.gallery.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SettingsData(
    var directory: String? = null,
    var galleryViewType: GalleryViewType = GalleryViewType.FOLDERS,
)