
package com.airmouse.presentation.ui.about

data class AboutUiState(
    val appName: String = "AirMouse Pro",
    val versionName: String = "1.0.1",
    val versionCode: Int = 2,
    val buildDate: String = "October 2026",
    val protocolVersion: String = "Protocol V2 (Canonical)",
    val totalDownloads: Int = 25000,
    val totalUsers: Int = 8400,
    val totalGestures: Int = 120000,
    val isUpdateAvailable: Boolean = false,
    val updateVersion: String? = null,
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
