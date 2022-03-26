package ru.dmitriyt.gallery.data.model

sealed class LoadingState<T> {
    class Loading<T> : LoadingState<T>()
    class Error<T> : LoadingState<T>()
    class Success<T>(val data: T) : LoadingState<T>()
}