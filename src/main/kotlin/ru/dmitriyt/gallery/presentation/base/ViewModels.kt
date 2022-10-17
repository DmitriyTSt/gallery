package ru.dmitriyt.gallery.presentation.base

import ru.dmitriyt.gallery.presentation.gallery.GalleryViewModel
import ru.dmitriyt.gallery.presentation.items.PhotoItemViewModel

object ViewModelStorage {

    private val storage = mutableMapOf<Class<*>, HashMap<String?, BaseViewModel>>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrCreate(hash: String?, clazz: Class<T>, constructor: () -> T): T {
        val viewModel = storage[clazz]?.get(hash)
        return if (viewModel != null) {
            viewModel as T
        } else {
            val newViewModel = constructor()
            if (storage[clazz] == null) {
                storage[clazz] = hashMapOf()
            }
            storage[clazz]?.set(hash, newViewModel as BaseViewModel)
            newViewModel
        }
    }
}

inline fun <reified T : BaseViewModel> viewModels(hash: String? = null): T {
    return when (T::class.java) {
        GalleryViewModel::class.java -> {
            ViewModelStorage.getOrCreate(hash, GalleryViewModel::class.java) {
                GalleryViewModel()
            } as T
        }
        else -> throw IllegalStateException("Unknown viewModel ${T::class.java}")
    }
}