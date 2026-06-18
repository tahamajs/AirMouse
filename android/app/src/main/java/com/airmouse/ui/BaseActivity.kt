package com.airmouse.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.airmouse.utils.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Base Activity for all UI screens in Air Mouse app.
 * Provides common functionality:
 * - Permission handling
 * - Loading states
 * - Error handling
 * - Theme management
 * - Debug overlay support
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var prefs: PreferencesManager

    // Debug overlay
    private var debugOverlay: DebugOverlay? = null
    protected var isDebugOverlayEnabled = false

    // Loading state
    private var loadingView: View? = null
    protected var isShowingLoading = false

    companion object {
        private const val DEBUG_OVERLAY_PREF = "debug_overlay_enabled"
    }

    // FIXED: Permission Launchers must be initialized during creation phase, not dynamically in a function
    private var singlePermissionCallback: (() -> Unit)? = null
    private var singlePermissionDeniedCallback: (() -> Unit)? = null
    private val requestSinglePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            singlePermissionCallback?.invoke()
        } else {
            singlePermissionDeniedCallback?.invoke() ?: showToast("Permission denied")
        }
    }

    private var multiplePermissionsCallback: (() -> Unit)? = null
    private var multiplePermissionsDeniedCallback: (() -> Unit)? = null
    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            multiplePermissionsCallback?.invoke()
        } else {
            multiplePermissionsDeniedCallback?.invoke() ?: showToast("Some permissions were denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyTheme()
        super.onCreate(savedInstanceState)

        prefs = PreferencesManager(this)
        isDebugOverlayEnabled = prefs.getBoolean(DEBUG_OVERLAY_PREF, false)

        initDebugOverlay()
    }

    override fun onResume() {
        super.onResume()
        if (isDebugOverlayEnabled) {
            debugOverlay?.show()
        }
    }

    override fun onPause() {
        super.onPause()
        debugOverlay?.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        debugOverlay?.destroy()
        hideLoading()
    }

    // ==================== THEME MANAGEMENT ====================

    private fun applyTheme() {
        val theme = prefs.getTheme()
        setTheme(when (theme) {
            "dark" -> com.airmouse.R.style.Theme_AirMouse_Dark
            "light" -> com.airmouse.R.style.Theme_AirMouse_Light
            else -> com.airmouse.R.style.Theme_AirMouse
        })
    }

    protected fun refreshTheme() {
        recreate()
    }

    // ==================== DEBUG OVERLAY ====================

    private fun initDebugOverlay() {
        debugOverlay = DebugOverlay(this).apply {
            initialize()
        }
    }

    protected fun toggleDebugOverlay() {
        isDebugOverlayEnabled = !isDebugOverlayEnabled
        prefs.putBoolean(DEBUG_OVERLAY_PREF, isDebugOverlayEnabled)

        if (isDebugOverlayEnabled) {
            debugOverlay?.show()
            showToast("Debug overlay enabled")
        } else {
            debugOverlay?.hide()
            showToast("Debug overlay disabled")
        }
    }

    protected fun setSensorServiceForDebug(service: com.airmouse.sensors.SensorService) {
        debugOverlay?.setSensorService(service)
    }

    // ==================== PERMISSION HANDLING ====================

    protected fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    protected fun requestPermission(
        permission: String,
        onGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        if (checkPermission(permission)) {
            onGranted()
            return
        }
        singlePermissionCallback = onGranted
        singlePermissionDeniedCallback = onDenied
        requestSinglePermissionLauncher.launch(permission)
    }

    protected fun requestMultiplePermissions(
        permissions: Array<String>,
        onAllGranted: () -> Unit,
        onDenied: (() -> Unit)? = null
    ) {
        val missingPermissions = permissions.filter { !checkPermission(it) }.toTypedArray()

        if (missingPermissions.isEmpty()) {
            onAllGranted()
            return
        }

        multiplePermissionsCallback = onAllGranted
        multiplePermissionsDeniedCallback = onDenied
        requestMultiplePermissionsLauncher.launch(missingPermissions)
    }

    // ==================== LOADING STATE ====================

    protected fun showLoading(containerId: Int = android.R.id.content) {
        if (isShowingLoading) return

        val container = findViewById<ViewGroup>(containerId) ?: return
        loadingView = LayoutInflater.from(this).inflate(
            com.airmouse.R.layout.view_loading,
            container,
            false
        )
        container.addView(loadingView)
        isShowingLoading = true
    }

    protected fun hideLoading() {
        loadingView?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            loadingView = null
        }
        isShowingLoading = false
    }

    // ==================== TOAST & SNACKBAR ====================

    protected fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }

    protected fun showToast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, getString(resId), duration).show()
    }

    protected fun showSnackbar(message: String, actionText: String? = null, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }

    // ==================== DIALOGS ====================

    protected fun showConfirmDialog(
        title: String,
        message: String,
        onConfirm: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel") { _, _ -> onCancel?.invoke() }
            .show()
    }

    protected fun showInfoDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // ==================== UTILITIES ====================

    protected fun <T> safeCall(block: () -> T, onError: ((Exception) -> Unit)? = null): T? {
        return try {
            block()
        } catch (e: Exception) {
            e.printStackTrace()
            onError?.invoke(e)
            showToast("Error: ${e.message}")
            null
        }
    }

    protected fun setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    )
        }
    }

    protected fun setStatusBarColor(colorResId: Int) {
        window.statusBarColor = ContextCompat.getColor(this, colorResId)
        val isLight = isColorLight(ContextCompat.getColor(this, colorResId))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                if (isLight) {
                    controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                } else {
                    controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                    )
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (isLight) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(color)) / 255
        return darkness < 0.5
    }

    // ==================== NETWORK STATUS ====================

    protected fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // ==================== LIFECYCLE SCOPE HELPERS ====================

    protected fun launchCoroutine(block: suspend () -> Unit) {
        lifecycleScope.launch {
            block()
        }
    }
}