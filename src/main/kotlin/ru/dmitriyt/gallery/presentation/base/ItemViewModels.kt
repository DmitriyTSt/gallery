package ru.dmitriyt.gallery.presentation.base

import ru.dmitriyt.gallery.presentation.items.PhotoItemViewModel

private const val MAX_ENTRY_SIZE = 100

object ItemViewModelStorage {

    private val storage = mutableMapOf<Class<*>, Entry>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrCreate(hash: String?, clazz: Class<T>, constructor: () -> T): T {
        val viewModel = storage[clazz]?.viewModels?.get(hash)
        return if (viewModel != null) {
            viewModel as T
        } else {
            val newViewModel = constructor()
            val entry = storage[clazz] ?: run {
                val newEntry = Entry()
                storage[clazz] = newEntry
                newEntry
            }
            entry.keys.addLast(hash)
            if (entry.keys.size > MAX_ENTRY_SIZE) {
                val hashToDelete = entry.keys.removeFirst()
                entry.viewModels[hashToDelete]?.clear()
                entry.viewModels.remove(hashToDelete)
            }
            entry.viewModels[hash] = newViewModel as BaseViewModel
            newViewModel
        }
    }

    data class Entry(
        val keys: ArrayDeque<String?> = ArrayDeque(),
        val viewModels: MutableMap<String?, BaseViewModel> = mutableMapOf(),
    )
}

inline fun <reified T : BaseViewModel> itemViewModels(hash: String? = null): T {
    return when (T::class.java) {
        PhotoItemViewModel::class.java -> {
            ItemViewModelStorage.getOrCreate(hash, PhotoItemViewModel::class.java) {
                PhotoItemViewModel()
            } as T
        }
        else -> throw IllegalStateException("Unknown itemViewModel ${T::class.java}")
    }
}