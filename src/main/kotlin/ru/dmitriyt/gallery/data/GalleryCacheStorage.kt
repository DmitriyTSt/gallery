package ru.dmitriyt.gallery.data

import androidx.compose.ui.graphics.ImageBitmap
import java.util.LinkedList

object GalleryCacheStorage {
    private const val CACHE_SIZE = 100
    private val cacheMap = mutableMapOf<String, ImageBitmap>()
    private val keys = LinkedList<String>()

    @Synchronized
    fun addToCache(key: String, image: ImageBitmap) {
        if (keys.size >= CACHE_SIZE) {
            val keyToRemove = keys.pop()
            cacheMap.remove(keyToRemove)
        }
        keys.add(key)
        cacheMap[key] = image
    }

    @Synchronized
    fun getFromCache(key: String): ImageBitmap? {
        return cacheMap[key]
    }
}