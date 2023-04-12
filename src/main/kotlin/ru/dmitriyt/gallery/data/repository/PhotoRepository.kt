package ru.dmitriyt.gallery.data.repository

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.File
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.imgscalr.Scalr
import ru.dmitriyt.gallery.data.GalleryCacheStorage
import ru.dmitriyt.gallery.presentation.util.ImageInformation
import ru.dmitriyt.gallery.presentation.util.ImageUtil

object PhotoRepository {

    suspend fun loadImagePreview(file: File, loadImagesContext: ExecutorCoroutineDispatcher): ImageBitmap {
        return GalleryCacheStorage.getFromFastCache(file.toString()) ?: run {
            val bufferedImage = GalleryCacheStorage.getFromFileCache(file.toString()) ?: run {
                withContext(Dispatchers.IO) {
                    val bufferedImage = ImageIO.read(file)
                    val resized = withContext(loadImagesContext) {
                        val resized = Scalr.resize(
                            bufferedImage,
                            Scalr.Method.SPEED,
                            Scalr.Mode.AUTOMATIC,
                            192 * 2,
                            192 * 2
                        )
                        resized
                    }
                    GalleryCacheStorage.addToFileCache(file.toString(), resized)
                    resized
                }
            }
            val imageInformation = ImageInformation.readImageInformation(file)
            val thumbnail = ImageUtil.fixImageByExif(
                bufferedImage,
                imageInformation.copy(width = bufferedImage.width, height = bufferedImage.height),
            )
            val newImage = thumbnail.toComposeImageBitmap()
            GalleryCacheStorage.addToFastCache(file.toString(), newImage)
            newImage
        }
    }

    suspend fun loadImage(file: File): ImageBitmap {
        return GalleryCacheStorage.getFromFastCacheFull(file.toString()) ?: run {
            withContext(Dispatchers.IO) {
                val bufferedImage = ImageIO.read(file)
                val imageInformation = ImageInformation.readImageInformation(file)
                val fixedOrientation = ImageUtil.fixImageByExif(
                    bufferedImage,
                    imageInformation.copy(width = bufferedImage.width, height = bufferedImage.height),
                )
                val image = fixedOrientation.toComposeImageBitmap()
                GalleryCacheStorage.addToFastCacheFull(file.toString(), image)
                image
            }
        }
    }
}