// app/src/main/java/com/airmouse/presentation/ui/about/AboutUiState.kt
package com.airmouse.presentation.ui.about

data class AboutUiState(
    val appName: String = "Air Mouse Pro",
    val versionName: String = "3.0.0",
    val versionCode: Int = 100,
    val buildDate: String = "",
    val isUpdateAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)