package com.airmouse.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<State, Event>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event.asSharedFlow()

    protected fun setState(reducer: State.() -> State) {
        _state.update { it.reducer() }
    }

    protected fun sendEvent(event: Event) {
        viewModelScope.launch { _event.emit(event) }
    }

    protected fun <T> launch(
        block: suspend CoroutineScope.() -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Job = viewModelScope.launch {
        runCatching { block() }
            .onSuccess(onSuccess)
            .onFailure(onError)
    }

    abstract fun onEvent(event: Event)
}

data class UiState<T>(
    val status: Status = Status.IDLE,
    val data: T? = null,
    val error: String? = null,
    val isLoading: Boolean = false
) {
    enum class Status { IDLE, LOADING, SUCCESS, ERROR }

    fun isLoading(): Boolean = status == Status.LOADING
    fun isSuccess(): Boolean = status == Status.SUCCESS
    fun isError(): Boolean = status == Status.ERROR

    companion object {
        fun <T> idle() = UiState<T>(status = Status.IDLE)
        fun <T> loading(data: T? = null) = UiState<T>(status = Status.LOADING, data = data)
        fun <T> success(data: T) = UiState<T>(status = Status.SUCCESS, data = data)
        fun <T> error(error: String, data: T? = null) = UiState<T>(status = Status.ERROR, data = data, error = error)
    }
}
