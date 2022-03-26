package ru.dmitriyt.gallery.data.model

import java.io.File

sealed class GalleryItem {
    class Photo(val file: File) : GalleryItem()
    class Directory(val file: File) : GalleryItem()
    class MonthDivider(val title: String) : GalleryItem()
}