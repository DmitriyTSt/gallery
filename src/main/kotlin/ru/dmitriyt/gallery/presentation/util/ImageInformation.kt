package ru.dmitriyt.gallery.presentation.util

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.MetadataException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.jpeg.JpegDirectory
import java.io.File

data class ImageInformation(
    val orientation: Int,
    val width: Int,
    val height: Int,
) {
    companion object {
        fun readImageInformation(imageFile: File): ImageInformation {
            val metadata = ImageMetadataReader.readMetadata(imageFile)
            val directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
            val jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory::class.java)
            var orientation = 1
            try {
                orientation = directory?.getInt(ExifIFD0Directory.TAG_ORIENTATION) ?: 1
            } catch (_: MetadataException) {
            }
            return ImageInformation(orientation, jpegDirectory?.imageWidth ?: 100, jpegDirectory?.imageHeight ?: 100)
        }
    }
}