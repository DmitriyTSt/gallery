package ru.dmitriyt.gallery.presentation.util

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.MetadataException
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.jpeg.JpegDirectory
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ImageInformation(
    val orientation: Int,
    val width: Int,
    val height: Int,
) {
    companion object {
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy:MM:dd hh:mm:ss")

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

        fun getPhotoCreationTime(imageFile: File): LocalDateTime {
            return Files.readAttributes(imageFile.toPath(), BasicFileAttributes::class.java)
                .lastModifiedTime()
                .toInstant()
                .let { LocalDateTime.ofInstant(it, ZoneId.systemDefault()) }
            // очень долго
//            val metadata = ImageMetadataReader.readMetadata(imageFile)
//            val directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java)
//            val stringDate = directory?.getString(ExifIFD0Directory.TAG_DATETIME_ORIGINAL)
//            return stringDate?.let { LocalDateTime.parse(stringDate, dateTimeFormatter) } ?: LocalDateTime.MIN
        }
    }
}