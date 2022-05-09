package ru.dmitriyt.gallery.presentation.resources

interface Strings {
    val appName: String get() = "Gallery"
    val galleryTitle: String
    val selectDirectoryTitle: String
    val selectDirectoryLabel: String
    val exitLabel: String
    val changeLabel: String
    val selectLabel: String
    fun galleryDirectoryTitle(vararg args: String): String
}

object RuStrings : Strings {
    override val galleryTitle = "Галерея"
    override val selectDirectoryTitle = "Выбор директории"
    override val selectDirectoryLabel = "Выбрать директорию"
    override val exitLabel = "Выйти"
    override val changeLabel = "Изменить"
    override val selectLabel = "Выбрать"
    override fun galleryDirectoryTitle(vararg args: String): String {
        return "Галерея %s".format(*args)
    }
}

object EnStrings : Strings {
    override val galleryTitle = "Gallery"
    override val selectDirectoryTitle = "Directory selection"
    override val selectDirectoryLabel = "Select directory"
    override val exitLabel = "Exit"
    override val changeLabel = "Change"
    override val selectLabel = "Select"
    override fun galleryDirectoryTitle(vararg args: String): String {
        return "Gallery %s".format(*args)
    }
}