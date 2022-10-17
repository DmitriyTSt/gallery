package ru.dmitriyt.gallery.presentation.base

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import ru.dmitriyt.gallery.data.model.LoadingState

abstract class BaseViewModel {
    protected val viewModelScope = CoroutineScope(Dispatchers.Main)

    protected fun <T> executeFlow(defaultValue: T? = null, block: suspend () -> T): Flow<LoadingState<T>> = flow {
        emit(defaultValue?.let { LoadingState.Success(it) } ?: LoadingState.Loading())
        try {
            val data = block()
            emit(LoadingState.Success(data))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(LoadingState.Error(e.message))
        }
    }

    fun <T, D> Flow<LoadingState<T>>.collectTo(destination: MutableStateFlow<D>, mapper: (LoadingState<T>) -> D) {
        viewModelScope.launch {
            collectLatest { destination.value = mapper(it) }
        }
    }

    fun <T> Flow<LoadingState<T>>.collectTo(destination: MutableStateFlow<LoadingState<T>>) {
        viewModelScope.launch {
            collectLatest {
                destination.value = it
            }
        }
    }

    fun clear() {
        viewModelScope.cancel()
    }
}