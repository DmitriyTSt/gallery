package ru.dmitriyt.gallery.data

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.util.LinkedList

object GalleryCacheStorage {
    private const val CACHE_SIZE = 100
    private const val CACHE_FULL_SIZE = 30
    private val cacheMap = mutableMapOf<String, ImageBitmap>()
    private val cacheFullMap = mutableMapOf<String, ImageBitmap>()
    private val keys = LinkedList<String>()
    private val keysFull = LinkedList<String>()

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
        return@withContext Unit
//        val base64key = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8)).takeLast(15) + ".jpg"
//        File(CACHE_DIR).apply {
//            if (!exists()) {
//                mkdir()
//            }
//        }
//        val imageFile = File(CACHE_DIR, base64key).apply {
//            if (!exists()) {
//                createNewFile()
//            }
//        }
//        ImageIO.write(image, "jpeg", imageFile)
//        println("saved to ${File(CACHE_DIR, base64key)}")
    }

    suspend fun getFromFileCache(key: String): BufferedImage? = withContext(Dispatchers.IO) {
        return@withContext null
//        val base64key = Base64.getEncoder().encodeToString(key.toByteArray(Charsets.UTF_8)).takeLast(15) + ".jpg"
//        val file = File(CACHE_DIR, base64key)
//        if (file.exists()) {
////            println("$file")
//            ImageIO.read(file)
//        } else {
//            null
//        }
    }
}