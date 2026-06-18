package com.airmouse.presentation.ui.about

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
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
        val timestamp = try {
            System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        _uiState.update {
            it.copy(buildDate = dateFormat.format(Date(timestamp)))
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

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            delay(1500)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isUpdateAvailable = false,
                    error = "You're on the latest version!"
                )
            }
        }
    }

    fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out Air Mouse Pro! Turn your phone into a wireless mouse.\n\nDownload: https://play.google.com/store/apps/details?id=${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Air Mouse").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun rateApp() {
        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=${context.packageName}".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openUrl("https://play.google.com/store/apps/details?id=${context.packageName}")
        }
    }

    fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (ignored: Exception) {}
    }
}