package com.airmouse.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.airmouse.utils.PreferencesManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var prefs: PreferencesManager

    
    private var debugOverlay: DebugOverlay? = null
    protected var isDebugOverlayEnabled = false

    
    private var loadingView: View? = null
    protected var isShowingLoading = false

    companion object {
        private const val DEBUG_OVERLAY_PREF = "debug_overlay_enabled"
    }

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
        isDebugOverlayEnabled = prefs.getBoolean(DEBUG_OVERLAY_PREF, defaultValue = false)

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

    

    protected fun setFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    protected fun setStatusBarColor(colorResId: Int) {
        window.statusBarColor = ContextCompat.getColor(this, colorResId)
        val isLight = isColorLight(ContextCompat.getColor(this, colorResId))

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = isLight
    }

    private fun isColorLight(color: Int): Boolean {
        val darkness = 1 - (0.299 * android.graphics.Color.red(color) + 0.587 * android.graphics.Color.green(color) + 0.114 * android.graphics.Color.blue(color)) / 255
        return darkness < 0.5
    }

    protected fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    protected fun launchCoroutine(block: suspend () -> Unit) {
        lifecycleScope.launch {
            block()
        }
    }
}
