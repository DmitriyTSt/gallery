package ru.dmitriyt.gallery.data

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.SettingsData

object Settings {
    private const val CACHE_DIR = ".gallery_cache"

    private val appDir by lazy {
        File(System.getProperty("user.home"), ".Gallery").apply {
            if (!exists()) {
                mkdir()
            }
        }
    }

    val cacheDir by lazy {
        File(appDir, CACHE_DIR).apply {
            if (!exists()) {
                mkdir()
            }
        }
    }

    private val settingsFile = getOrCreatePreferencesFile()

    private var settings: SettingsData
        get() = Json.decodeFromString(settingsFile.readText())
        set(value) {
            settingsFile.writeText(Json.encodeToString(value))
        }

    var directory: File?
        get() = settings.directory?.let { File(it) }
        set(value) {
            settings = settings.copy(directory = value?.absolutePath)
        }

    var galleryViewType: GalleryViewType
        get() = settings.galleryViewType
        set(value) {
            settings = settings.copy(galleryViewType = value)
        }

    private fun getOrCreatePreferencesFile(): File {
        return File(appDir, "settings.json").apply {
            if (!exists()) {
                createNewFile()
                writeText("{}")
            }
        }
    }
}