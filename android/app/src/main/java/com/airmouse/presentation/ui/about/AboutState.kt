
package com.airmouse.presentation.ui.about

import androidx.compose.ui.graphics.vector.ImageVector

data class AboutUiState(
    val appName: String = "Air Mouse",
    val versionName: String = "3.0.0",
    val versionCode: Int = 30,
    val buildDate: String = "2025-06-20",
    val totalDownloads: Int = 12500,
    val totalUsers: Int = 3400,
    val totalGestures: Int = 87500,
    val isUpdateAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class AboutEvent {
    object ShareApp : AboutEvent()
    object RateApp : AboutEvent()
    object CheckForUpdates : AboutEvent()
    data class OpenUrl(val url: String) : AboutEvent()
    object NavigateBack : AboutEvent()
}

sealed class AboutEffect {
    data class OpenUrl(val url: String) : AboutEffect()
    data class ShowToast(val message: String) : AboutEffect()
    object NavigateBack : AboutEffect()
    object ShowUpdateDialog : AboutEffect()
}