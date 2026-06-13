// app/src/main/java/com/airmouse/presentation/ui/about/AboutViewModel.kt
package com.airmouse.presentation.ui.about

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        loadAppInfo()
    }

    private fun loadAppInfo() {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "3.0.0"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }

            // Build date from APK timestamp (fallback)
            val apkFile = context.applicationInfo.sourceDir
            val apkLastModified = java.io.File(apkFile).lastModified()
            val buildDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(apkLastModified))

            _uiState.update {
                it.copy(
                    versionName = versionName,
                    versionCode = versionCode,
                    buildDate = buildDate,
                    errorMessage = null
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            _uiState.update {
                it.copy(errorMessage = "Could not load app information")
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            // Simulate network check – replace with actual API call if needed
            kotlinx.coroutines.delay(1500)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isUpdateAvailable = false // Placeholder: you can implement real update check
                )
            }
        }
    }
}