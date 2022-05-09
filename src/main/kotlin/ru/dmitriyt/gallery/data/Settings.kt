package ru.dmitriyt.gallery.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.dmitriyt.gallery.data.model.GalleryViewType
import ru.dmitriyt.gallery.data.model.SettingsData
import java.io.File

object Settings {
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
        return File("settings.json").apply {
            if (!exists()) {
                createNewFile()
                writeText("{}")
            }
        }
    }
}