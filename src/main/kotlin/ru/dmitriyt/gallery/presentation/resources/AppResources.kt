package ru.dmitriyt.gallery.presentation.resources

import java.util.Locale

object AppResources {
    const val appIcon = "gallery.png"
    private var language: String = "en"

    fun strings(): Strings {
        return when (language) {
            "ru" -> RuStrings
            else -> EnStrings
        }
    }

    fun locale(): Locale {
        return when (language) {
            "ru" -> Locale("ru", "RU")
            else -> Locale.ENGLISH
        }
    }
}