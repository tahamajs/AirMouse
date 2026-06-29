package com.airmouse.presentation.ui.notifications

import androidx.lifecycle.ViewModel
import com.airmouse.notifications.NotificationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class NotificationsCenterUiState(
    val connectionCount: Int = 0,
    val errorCount: Int = 0,
    val calibrationCount: Int = 0,
    val lastMessage: String = ""
)

@HiltViewModel
class NotificationsCenterViewModel @Inject constructor(
    private val notificationManager: NotificationManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationsCenterUiState())
    val uiState: StateFlow<NotificationsCenterUiState> = _uiState.asStateFlow()

    fun sendTestNotification() {
        notificationManager.showInfoNotification(
            title = "Air Mouse",
            message = "Test notification from the notifications center"
        )
        _uiState.value = _uiState.value.copy(lastMessage = "Test notification sent")
    }
}
