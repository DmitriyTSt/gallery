package ru.dmitriyt.gallery.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.toBitmap
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.LinkedList
import javax.imageio.ImageIO

object GalleryCacheStorage {
    private const val CACHE_SIZE = 100
    private val cacheMap = mutableMapOf<String, ImageBitmap>()
    private val keys = LinkedList<String>()

    private const val CACHE_DIR = ".gallery_cache"

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

    suspend fun addToFileCache(key: String, image: BufferedImage) = withContext(Dispatchers.IO) {
        val base64key = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8)).takeLast(15) + ".jpg"
        File(CACHE_DIR).apply {
            if (!exists()) {
                mkdir()
            }
        }
        val imageFile = File(CACHE_DIR, base64key).apply {
            if (!exists()) {
                createNewFile()
            }
        }
        ImageIO.write(image, "jpeg", imageFile)
//        println("saved to ${File(CACHE_DIR, base64key)}")
    }

    suspend fun getFromFileCache(key: String): BufferedImage? = withContext(Dispatchers.IO) {
        val base64key = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8)).takeLast(15) + ".jpg"
        val file = File(CACHE_DIR, base64key)
        if (file.exists()) {
//            println("$file")
            ImageIO.read(file)
        } else {
            null
        }
    }
}