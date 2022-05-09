package ru.dmitriyt.gallery.data.repository

import ru.dmitriyt.gallery.data.model.GalleryItem
import ru.dmitriyt.gallery.presentation.resources.AppResources
import ru.dmitriyt.gallery.presentation.util.ImageInformation
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object PhotoRepository {
    suspend fun getPhotosWithDateSort(directory: File): List<GalleryItem> = suspendCoroutine { continuation ->
        val fileToAttrs = getAllFilesRecursive(directory)
            .map { it to ImageInformation.getPhotoCreationTime(it) }
            .sortedByDescending { (_, creatingDateTime) ->
                creatingDateTime
            }
        val items = mutableListOf<GalleryItem>()
        val monthFormat = SimpleDateFormat("LLLL yyyy", AppResources.locale())
        fileToAttrs.forEachIndexed { index, (file, creatingDateTime) ->
            if (index == 0 || !isSameMonths(creatingDateTime, fileToAttrs[index - 1].second)) {
                items.add(
                    GalleryItem.MonthDivider(
                        monthFormat
                            .format(creatingDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    )
                )
            }
            items.add(GalleryItem.Photo(file))
        }
        continuation.resume(items)
    }

    suspend fun getPhotoDirectories(currentDirectory: File): List<GalleryItem> = suspendCoroutine { continuation ->
        val result = currentDirectory.listImages(withDirs = true).map { file ->
            if (file.isDirectory) {
                GalleryItem.Directory(file)
            } else {
                GalleryItem.Photo(file)
            }
        }
        continuation.resume(result)
    }

    private fun isSameMonths(first: LocalDateTime, second: LocalDateTime): Boolean {
        return first.monthValue == second.monthValue && first.year == second.year
    }

    private fun getAllFilesRecursive(directory: File): List<File> {
        val dirFiles = directory.listImages(withDirs = true)
        val allFiles = mutableListOf<File>()
        dirFiles.forEach { dirFile ->
            if (dirFile.isDirectory) {
                allFiles.addAll(getAllFilesRecursive(dirFile))
            } else {
                allFiles.add(dirFile)
            }
        }
        return allFiles
    }

    private fun File.listImages(withDirs: Boolean): List<File> {
        return listFiles()?.toList().orEmpty().filter { (if (withDirs) it.isDirectory else false) || it.isImage() }
    }

    private fun File.isImage(): Boolean {
        return setOf("jpg", "png", "bmp", "webp", "ico", "gif", "jpeg").contains(this.extension.lowercase())
    }
}