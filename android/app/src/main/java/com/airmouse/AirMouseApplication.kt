// app/src/main/java/com/airmouse/AirMouseApplication.kt
package com.airmouse

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfiguration
import com.airmouse.BuildConfig // ✅ IMPORT ADDED
import com.airmouse.utils.PreferencesManager
import com.airmouse.utils.LogManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AirMouseApplication : Application(), WorkConfiguration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prefsManager: PreferencesManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        lateinit var instance: AirMouseApplication
            private set

        fun getAppContext(): android.content.Context = instance.applicationContext

        const val APP_VERSION = "4.9.9"
        const val APP_NAME = "Air Mouse Pro"
        const val BUILD_NUMBER = 30
        const val PREF_APP_START_TIME = "app_start_time"
        const val PREF_LAST_EXIT_TIME = "last_exit_time"
        const val PREF_TOTAL_UPTIME = "total_uptime"
        const val PREF_APP_LAUNCH_COUNT = "app_launch_count"
        const val PREF_FIRST_LAUNCH = "first_launch"
    }

    private var isAppInForeground = false
    private var activeActivities = 0
    private var appStartTime = 0L
    private var crashReportingInitialized = false
    private var isFirstLaunch = true
    private var launchCount = 0

    // ==========================================
    // LIFECYCLE METHODS
    // ==========================================

    override fun onCreate() {
        super.onCreate()
        instance = this
        appStartTime = System.currentTimeMillis()

        // Initialize core components
        LogManager.init(this)
        initLogging()
        initCrashReporting()
        initPreferences()
        applyTheme()

        // Enable strict mode in debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks()

        // Set up uncaught exception handler
        setupUncaughtExceptionHandler()

        // Create notification channels
        createNotificationChannels()

        // Track app launch
        trackAppLaunch()

        Timber.i("✅ Air Mouse Application initialized - Version $APP_VERSION (Build $BUILD_NUMBER)")
        Timber.d("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
        Timber.d("First Launch: $isFirstLaunch, Launch Count: $launchCount")
    }

    // ==========================================
    // INITIALIZATION METHODS
    // ==========================================

    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }
    }

    private fun initCrashReporting() {
        if (!BuildConfig.DEBUG && !crashReportingInitialized) {
            try {
                crashReportingInitialized = true
                // Uncomment when you have a crash reporting library
                // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                Timber.i("Crash reporting initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize crash reporting")
            }
        }
    }

    private fun initPreferences() {
        isFirstLaunch = prefsManager.getBoolean(PREF_FIRST_LAUNCH, true)
        launchCount = prefsManager.getInt(PREF_APP_LAUNCH_COUNT, 0)

        if (isFirstLaunch) {
            prefsManager.putBoolean(PREF_FIRST_LAUNCH, false)
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyDeathOnNetwork()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .detectFileUriExposure()
                .penaltyLog()
                .build()
        )
    }

    private fun applyTheme() {
        try {
            val theme = prefsManager.getString("theme", "system")
            when (theme) {
                "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                "dark", "pure_black" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "high_contrast" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply theme")
        }
    }

    private fun registerActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                activeActivities++
                if (!isAppInForeground && activeActivities == 1) {
                    isAppInForeground = true
                    onAppForeground()
                }
            }
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                activeActivities--
                if (isAppInForeground && activeActivities == 0) {
                    isAppInForeground = false
                    onAppBackground()
                }
            }
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "💀 Uncaught exception in thread: ${thread.name}")
            // Log crash to crash reporting
            // FirebaseCrashlytics.getInstance().recordException(throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        try {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val channels = mutableListOf<NotificationChannel>()

            // Connection Channel
            channels.add(
                NotificationChannel(
                    "connection_channel",
                    "Connection Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows connection status and network updates"
                    setSound(null, null)
                    enableVibration(false)
                }
            )

            // Error Channel
            channels.add(
                NotificationChannel(
                    "error_channel",
                    "Errors",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Error notifications"
                    enableVibration(true)
                }
            )

            // General Channel
            channels.add(
                NotificationChannel(
                    "general_channel",
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "General notifications"
                }
            )

            // Gesture Channel
            channels.add(
                NotificationChannel(
                    "gesture_channel",
                    "Gestures",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Shows gesture detection notifications"
                    enableVibration(true)
                }
            )

            // Proximity Channel
            channels.add(
                NotificationChannel(
                    "proximity_channel",
                    "Proximity",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Shows proximity lock/unlock notifications"
                    enableVibration(true)
                    enableLights(true)
                    lightColor = 0xFFFF5722.toInt()
                }
            )

            // Calibration Channel
            channels.add(
                NotificationChannel(
                    "calibration_channel",
                    "Calibration",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Shows calibration progress and status"
                    enableVibration(false)
                }
            )

            // Voice Channel
            channels.add(
                NotificationChannel(
                    "voice_channel",
                    "Voice Commands",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows voice command status"
                    setSound(null, null)
                }
            )

            // Update Channel
            channels.add(
                NotificationChannel(
                    "update_channel",
                    "App Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Shows app update notifications"
                    enableVibration(true)
                }
            )

            manager.createNotificationChannels(channels)
            Timber.i("✅ Created ${channels.size} notification channels")
        } catch (e: Exception) {
            Timber.e(e, "Failed to create notification channels")
        }
    }

    private fun trackAppLaunch() {
        launchCount++
        prefsManager.putInt(PREF_APP_LAUNCH_COUNT, launchCount)
        prefsManager.putLong(PREF_APP_START_TIME, appStartTime)
        Timber.d("App launch count: $launchCount")
    }

    // ==========================================
    // FOREGROUND / BACKGROUND HANDLING
    // ==========================================

    private fun onAppForeground() {
        Timber.i("🟢 App moved to foreground")
    }

    private fun onAppBackground() {
        Timber.i("🔴 App moved to background")
    }

    // ==========================================
    // CONFIGURATION CHANGES
    // ==========================================

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
        Timber.d("Configuration changed: ${newConfig.uiMode}")
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("⚠️ Low memory warning")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Trim memory: level=$level")
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                // App UI is hidden, release UI resources
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> {
                // Moderate memory pressure, release caches
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                // Low memory pressure, release more resources
            }
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                // Critical memory pressure, release everything possible
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                // App is in background, release memory
            }
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                // App is in background and memory is critically low
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                // App is in background and memory is moderate
            }
        }
    }

    // ==========================================
    // TERMINATION HANDLING
    // ==========================================

    override fun onTerminate() {
        super.onTerminate()
        val uptime = System.currentTimeMillis() - appStartTime
        try {
            // Save uptime data
            val totalUptime = prefsManager.getLong(PREF_TOTAL_UPTIME, 0L) + uptime
            prefsManager.putLong(PREF_TOTAL_UPTIME, totalUptime)
            prefsManager.putLong(PREF_LAST_EXIT_TIME, System.currentTimeMillis())

            // Cleanup
            cleanup()

            Timber.i("⏱️ Session uptime: ${uptime / 1000}s, Total uptime: ${totalUptime / 1000}s")
        } catch (_: Exception) {
            // Ignore on termination
        }
    }

    private fun cleanup() {
        try {
            // Release resources
            Timber.uprootAll()
        } catch (_: Exception) { /* ignore */ }
    }

    fun isAppInForegroundState(): Boolean = isAppInForeground

    fun getAppExecutionUptime(): Long = System.currentTimeMillis() - appStartTime

    fun getTotalUptime(): Long = prefsManager.getLong(PREF_TOTAL_UPTIME, 0L)

    fun getAppLaunchCount(): Int = prefsManager.getInt(PREF_APP_LAUNCH_COUNT, 0)

    fun isFirstLaunch(): Boolean = prefsManager.getBoolean(PREF_FIRST_LAUNCH, true)

    fun getBuildInfo(): String = "$APP_NAME v$APP_VERSION (Build $BUILD_NUMBER)"

    fun getDeviceInfo(): String = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
}

class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.ERROR) {
            if (t != null) {
                Timber.e(t, "[$tag] $message")
            } else {
                Timber.e("[$tag] $message")
            }
        } else if (priority == android.util.Log.WARN) {
            Timber.w("[$tag] $message")
        }
        // Other priorities are ignored for this tree; you can add more if needed.
    }
}