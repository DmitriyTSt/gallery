package ru.dmitriyt.gallery.data

import androidx.compose.ui.graphics.ImageBitmap
import java.awt.image.BufferedImage
import java.io.File
import java.util.Base64
import java.util.LinkedList
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GalleryCacheStorage {
    private const val CACHE_SIZE = 100
    private const val CACHE_FULL_SIZE = 30
    private val cacheMap = mutableMapOf<String, ImageBitmap>()
    private val cacheFullMap = mutableMapOf<String, ImageBitmap>()
    private val keys = LinkedList<String>()
    private val keysFull = LinkedList<String>()

    @Synchronized
    fun addToFastCache(key: String, image: ImageBitmap) {
        if (keys.size >= CACHE_SIZE) {
            val keyToRemove = keys.pop()
            cacheMap.remove(keyToRemove)
        }
        keys.add(key)
        cacheMap[key] = image
    }

    @Synchronized
    fun getFromFastCache(key: String): ImageBitmap? {
        return cacheMap[key]
    }

    @Synchronized
    fun addToFastCacheFull(key: String, image: ImageBitmap) {
        if (keysFull.size >= CACHE_FULL_SIZE) {
            val keyToRemove = keysFull.pop()
            cacheFullMap.remove(keyToRemove)
        }
        keysFull.add(key)
        cacheFullMap[key] = image
    }

    @Synchronized
    fun getFromFastCacheFull(key: String): ImageBitmap? {
        return cacheFullMap[key]
    }

    suspend fun addToFileCache(key: String, image: BufferedImage) = withContext(Dispatchers.IO) {
        val base64key = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8)).takeLast(15) + ".jpg"
        val imageFile = File(Settings.cacheDir, base64key).apply {
            if (!exists()) {
                createNewFile()
            }
        }
        ImageIO.write(image, "jpg", imageFile)
    }

    suspend fun getFromFileCache(key: String): BufferedImage? = withContext(Dispatchers.IO) {
        val base64key = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8)).takeLast(15) + ".jpg"
        val file = File(Settings.cacheDir, base64key)
        if (file.exists()) {
            ImageIO.read(file)
        } else {
            null
        }
    }
}