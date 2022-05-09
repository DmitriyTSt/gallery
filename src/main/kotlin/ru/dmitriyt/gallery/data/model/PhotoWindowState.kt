package ru.dmitriyt.gallery.data.model

import java.io.File

sealed class PhotoWindowState(
    val shown: Boolean
) {
    object Hidden : PhotoWindowState(false)
    data class Shown(
        val index: Int,
        val file: File,
        val name: String,
    ) : PhotoWindowState(true)
}