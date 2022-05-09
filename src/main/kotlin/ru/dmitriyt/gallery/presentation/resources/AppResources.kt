package ru.dmitriyt.gallery.presentation.resources

import java.util.Locale

object AppResources {
    private var language: String = "ru"

    /** Иконки */
    val icons = Icons

    /** Строки */
    fun strings(): Strings {
        return when (language) {
            "ru" -> RuStrings
            else -> EnStrings
        }
    }

    /** Локаль (для форматирования дат) */
    fun locale(): Locale {
        return when (language) {
            "ru" -> Locale("ru", "RU")
            else -> Locale.ENGLISH
        }
    }
}