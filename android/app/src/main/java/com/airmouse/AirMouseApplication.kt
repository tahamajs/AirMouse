package com.airmouse

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfiguration
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class AirMouseApplication : Application(), WorkConfiguration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prefsManager: PreferencesManager

    override val workManagerConfiguration: WorkConfiguration
        get() = WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    companion object {
        lateinit var instance: AirMouseApplication
            private set

        fun getAppContext(): android.content.Context = instance.applicationContext

        const val APP_VERSION = "3.0.0"
        const val APP_NAME = "Air Mouse Pro"
        const val BUILD_NUMBER = 30
    }

    private var isAppInForeground = false
    private var activeActivities = 0
    private var appStartTime = 0L
    private var crashReportingInitialized = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        appStartTime = System.currentTimeMillis()

        // Initialize logging
        initLogging()

        // Initialize crash reporting
        initCrashReporting()

        // Enable strict mode in debug builds
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Apply saved theme
        applyTheme()

        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks()

        // Set up uncaught exception handler
        setupUncaughtExceptionHandler()

        // Notification channels
        createNotificationChannels()

        Timber.i("✅ Air Mouse Application initialized - Version $APP_VERSION (Build $BUILD_NUMBER)")
        Timber.d("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
    }

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
                // CrashReporting.initialize(this) - uncomment when CrashReporting class exists
                Timber.i("Crash reporting initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize crash reporting")
            }
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
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
            val theme = prefsManager.getTheme()
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
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: android.app.Activity) {
                activeActivities++
                if (!isAppInForeground && activeActivities == 1) {
                    isAppInForeground = true
                    onAppForeground()
                }
            }
            override fun onActivityResumed(activity: android.app.Activity) {}
            override fun onActivityPaused(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {
                activeActivities--
                if (isAppInForeground && activeActivities == 0) {
                    isAppInForeground = false
                    onAppBackground()
                }
            }
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }

    private fun onAppForeground() {
        Timber.i("🟢 App moved to foreground")
    }

    private fun onAppBackground() {
        Timber.i("🔴 App moved to background")
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "💀 Uncaught exception in thread: ${thread.name}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun createNotificationChannels() {
        try {
            val manager = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
                val channels = listOf(
                    NotificationChannel(
                        "connection_channel",
                        "Connection Status",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "Shows connection status"
                        setSound(null, null)
                    },
                    NotificationChannel(
                        "gesture_channel",
                        "Gesture Detection",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Shows gesture detection notifications"
                    },
                    NotificationChannel(
                        "proximity_channel",
                        "Proximity Lock",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Shows proximity lock/unlock notifications"
                        enableVibration(true)
                    }
                )
                manager.createNotificationChannels(channels)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to create notification channels")
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("Low memory warning")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Trim memory: level=$level")
    }

    override fun onTerminate() {
        super.onTerminate()
        val uptime = System.currentTimeMillis() - appStartTime
        try {
            prefsManager.putLong("last_exit_time", System.currentTimeMillis())
            prefsManager.putLong("total_uptime", prefsManager.getLong("total_uptime", 0L) + uptime)
            cleanup()
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun cleanup() {
        try {
            Timber.uprootAll()
        } catch (_: Exception) { /* ignore */ }
    }

    fun isAppInForegroundState(): Boolean = isAppInForeground
    fun getAppExecutionUptime(): Long = System.currentTimeMillis() - appStartTime
}

class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.ERROR) {
            if (t != null) {
                // CrashReporting.logException(t) - uncomment when CrashReporting exists
                Timber.e(t, "[$tag] $message")
            } else {
                Timber.e("[$tag] $message")
            }
        }
    }
}