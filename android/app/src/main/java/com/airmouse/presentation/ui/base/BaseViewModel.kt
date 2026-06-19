// app/src/main/java/com/airmouse/presentation/base/BaseViewModel.kt
package com.airmouse.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseViewModel<State, Event>(initialState: State) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _event = MutableSharedFlow<Event>()
    val event: SharedFlow<Event> = _event.asSharedFlow()

    protected val uiScope = viewModelScope

    protected fun setState(reducer: State.() -> State) {
        _state.update { it.reducer() }
    }

    protected fun sendEvent(event: Event) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    protected fun <T> launch(
        block: suspend CoroutineScope.() -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ): Job {
        return viewModelScope.launch {
            try {
                val result = block()
                onSuccess(result)
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    protected suspend fun <T> safeCall(
        block: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    abstract fun onEvent(event: Event)
}

// UI State wrapper
data class UiState<T>(
    val status: Status = Status.IDLE,
    val data: T? = null,
    val error: String? = null,
    val isLoading: Boolean = false
) {
    enum class Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR// app/src/main/java/com/airmouse/presentation/base/BaseViewModel.kt
        package com.airmouse.presentation.base

        import androidx.lifecycle.ViewModel
        import androidx.lifecycle.viewModelScope
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.Dispatchers
        import kotlinx.coroutines.Job
        import kotlinx.coroutines.flow.*
        import kotlinx.coroutines.launch
        import kotlinx.coroutines.withContext

        abstract class BaseViewModel<State, Event>(initialState: State) : ViewModel() {

            private val _state = MutableStateFlow(initialState)
            val state: StateFlow<State> = _state.asStateFlow()

            private val _event = MutableSharedFlow<Event>()
            val event: SharedFlow<Event> = _event.asSharedFlow()

            protected val uiScope = viewModelScope

            protected fun setState(reducer: State.() -> State) {
                _state.update { it.reducer() }
            }

            protected fun sendEvent(event: Event) {
                viewModelScope.launch {
                    _event.emit(event)
                }
            }

            protected fun <T> launch(
                block: suspend CoroutineScope.() -> T,
                onSuccess: (T) -> Unit = {},
                onError: (Throwable) -> Unit = {}
            ): Job {
                return viewModelScope.launch {
                    try {
                        val result = block()
                        onSuccess(result)
                    } catch (e: Exception) {
                        onError(e)
                    }
                }
            }

            protected suspend fun <T> safeCall(
                block: suspend () -> T
            ): Result<T> {
                return try {
                    Result.success(block())
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            abstract fun onEvent(event: Event)
        }

        // UI State wrapper
        data class UiState<T>(
            val status: Status = Status.IDLE,
            val data: T? = null,
            val error: String? = null,
            val isLoading: Boolean = false
        ) {
            enum class Status {
                IDLE,
                LOADING,
                SUCCESS,
                ERROR
            }

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
    }

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