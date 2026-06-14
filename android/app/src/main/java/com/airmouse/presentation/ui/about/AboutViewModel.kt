package com.airmouse.presentation.ui.about

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.BuildConfig
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    data class AboutUiState(
        val appName: String = "Air Mouse Pro",
        val versionName: String = BuildConfig.VERSION_NAME,
        val versionCode: Int = BuildConfig.VERSION_CODE,
        val buildDate: String = "",
        val isUpdateAvailable: Boolean = false,
        val isLoading: Boolean = false,
        val totalDownloads: Int = 0,
        val totalUsers: Int = 0,
        val totalGestures: Int = 0,
        val error: String? = null
    )

    init {
        loadBuildDate()
        loadStats()
    }

    private fun loadBuildDate() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        _uiState.update {
            it.copy(buildDate = dateFormat.format(Date(BuildConfig.BUILD_TIMESTAMP)))
        }
    }

    private fun loadStats() {
        _uiState.update {
            it.copy(
                totalDownloads = prefs.getInt("total_downloads", 15000),
                totalUsers = prefs.getInt("active_users", 3200),
                totalGestures = prefs.getInt("total_gestures", 50000)
            )
        }
    }

    fun getAppVersion(): String = BuildConfig.VERSION_NAME

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(1500) // Simulate network call
            
            // In production, check against a remote API
            val hasUpdate = false // Replace with actual update check
            
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isUpdateAvailable = hasUpdate,
                    error = if (hasUpdate) null else "You're on the latest version!"
                )
            }
        }
    }

    fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out Air Mouse Pro! Turn your phone into a wireless mouse.\n\nDownload: https://play.google.com/store/apps/details?id=${context.packageName}")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Air Mouse"))
    }

    fun rateApp() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
        context.startActivity(intent)
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}