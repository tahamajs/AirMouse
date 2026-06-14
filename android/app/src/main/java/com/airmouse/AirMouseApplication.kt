package com.airmouse

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration as WorkConfiguration
import com.airmouse.utils.PreferencesManager
import com.airmouse.utils.crash.CrashReporting
import com.airmouse.utils.di.AppContainer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject

@HiltAndroidApp
class AirMouseApplication : Application(), WorkConfiguration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    
    companion object {
        lateinit var instance: AirMouseApplication
            private set
            
        fun getAppContext(): Context = instance.applicationContext
        
        // Application-wide constants
        const val APP_VERSION = "3.0.0"
        const val APP_NAME = "Air Mouse Pro"
        const val BUILD_NUMBER = 30
    }
    
    // App container for manual dependency injection (legacy support)
    lateinit var appContainer: AppContainer
        private set
    
    // Application state
    private var isAppInForeground = false
    private var activeActivities = 0
    private var appStartTime = 0L
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        appStartTime = System.currentTimeMillis()
        
        // Initialize app container
        appContainer = AppContainer(this)
        
        // Initialize PreferencesManager
        PreferencesManager.init(this)
        
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
        
        // Initialize background work
        initBackgroundWork()
        
        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks()
        
        // Set up uncaught exception handler
        setupUncaughtExceptionHandler()
        
        // Initialize notification channels (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels()
        }
        
        Timber.i("✅ Air Mouse Application initialized - Version $APP_VERSION (Build $BUILD_NUMBER)")
        Timber.d("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
    }
    
    private fun initLogging() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            Timber.d("Debug logging enabled")
        } else {
            Timber.plant(CrashReportingTree())
            Timber.d("Production logging enabled with crash reporting")
        }
    }
    
    private fun initCrashReporting() {
        if (!BuildConfig.DEBUG) {
            try {
                CrashReporting.initialize(this)
                Timber.d("Crash reporting initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize crash reporting")
            }
        }
    }
    
    private fun enableStrictMode() {
        // Thread policy
        ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .penaltyDeathOnNetwork()
            .build()
            .let { ThreadPolicy.setThreadPolicy(it) }
        
        // VM policy
        VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .detectActivityLeaks()
            .detectFileUriExposure()
            .penaltyLog()
            .build()
            .let { VmPolicy.setVmPolicy(it) }
        
        Timber.d("⚠️ StrictMode enabled (debug only)")
    }
    
    private fun applyTheme() {
        try {
            val prefs = PreferencesManager(this)
            val theme = prefs.getString("theme", "system")
            
            when (theme) {
                "light" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    Timber.d("Theme applied: Light")
                }
                "dark" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    Timber.d("Theme applied: Dark")
                }
                "pure_black" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    Timber.d("Theme applied: Pure Black")
                }
                "high_contrast" -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    Timber.d("Theme applied: High Contrast")
                }
                else -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    Timber.d("Theme applied: System default")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply theme")
        }
    }
    
    private fun initBackgroundWork() {
        try {
            // WorkManager is initialized by Hilt automatically
            // Additional configuration can be added here
            Timber.d("Background work initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize background work")
        }
    }
    
    private fun registerActivityLifecycleCallbacks() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) {
                Timber.v("Activity created: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityStarted(activity: android.app.Activity) {
                activeActivities++
                if (!isAppInForeground && activeActivities == 1) {
                    isAppInForeground = true
                    onAppForeground()
                }
                Timber.v("Activity started: ${activity.javaClass.simpleName} (Active: $activeActivities)")
            }
            
            override fun onActivityResumed(activity: android.app.Activity) {
                Timber.v("Activity resumed: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityPaused(activity: android.app.Activity) {
                Timber.v("Activity paused: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityStopped(activity: android.app.Activity) {
                activeActivities--
                if (isAppInForeground && activeActivities == 0) {
                    isAppInForeground = false
                    onAppBackground()
                }
                Timber.v("Activity stopped: ${activity.javaClass.simpleName} (Active: $activeActivities)")
            }
            
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) {
                Timber.v("Activity save instance: ${activity.javaClass.simpleName}")
            }
            
            override fun onActivityDestroyed(activity: android.app.Activity) {
                Timber.v("Activity destroyed: ${activity.javaClass.simpleName}")
            }
        })
    }
    
    private fun onAppForeground() {
        Timber.i("🟢 App moved to foreground")
        
        // Resume network monitoring
        try {
            appContainer.resumeNetworkMonitoring()
        } catch (e: Exception) {
            Timber.e(e, "Failed to resume network monitoring")
        }
        
        // Refresh connection status
        try {
            appContainer.refreshConnectionStatus()
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh connection status")
        }
        
        // Resume sensor collection if needed
        try {
            appContainer.resumeSensorCollection()
        } catch (e: Exception) {
            Timber.e(e, "Failed to resume sensor collection")
        }
    }
    
    private fun onAppBackground() {
        Timber.i("🔴 App moved to background")
        
        // Pause non-critical operations
        try {
            appContainer.pauseNonCriticalOperations()
        } catch (e: Exception) {
            Timber.e(e, "Failed to pause non-critical operations")
        }
        
        // Save current state
        try {
            appContainer.saveAppState()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save app state")
        }
        
        // Reduce network activity
        try {
            appContainer.reduceNetworkActivity()
        } catch (e: Exception) {
            Timber.e(e, "Failed to reduce network activity")
        }
    }
    
    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val stackTrace = getStackTraceString(throwable)
            Timber.e(throwable, "💀 Uncaught exception in thread: ${thread.name}\n$stackTrace")
            
            try {
                CrashReporting.logException(throwable)
                CrashReporting.logMessage("Uncaught exception in ${thread.name}")
            } catch (e: Exception) {
                // Ignore crash reporting errors
            }
            
            // Let the default handler handle the crash
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        Timber.d("Uncaught exception handler configured")
    }
    
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = getSystemService(NotificationManager::class.java)
                
                // Connection channel
                val connectionChannel = NotificationChannel(
                    "connection_channel",
                    "Connection Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows connection status notifications"
                    setSound(null, null)
                }
                
                // Gesture channel
                val gestureChannel = NotificationChannel(
                    "gesture_channel",
                    "Gesture Detection",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Shows gesture detection notifications"
                }
                
                // Proximity channel
                val proximityChannel = NotificationChannel(
                    "proximity_channel",
                    "Proximity Lock",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Shows proximity lock/unlock notifications"
                    enableVibration(true)
                }
                
                notificationManager.createNotificationChannels(
                    listOf(connectionChannel, gestureChannel, proximityChannel)
                )
                
                Timber.d("Notification channels created")
            } catch (e: Exception) {
                Timber.e(e, "Failed to create notification channels")
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.d("Configuration changed - Orientation: ${newConfig.orientation}, " +
                "Font scale: ${newConfig.fontScale}, Locale: ${newConfig.locale}")
        
        // Reapply theme if language changed
        if (newConfig.locale != resources.configuration.locale) {
            applyTheme()
            Timber.d("Theme reapplied due to locale change")
        }
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("⚠️ Low memory warning received")
        clearMemoryCaches()
    }
    
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        
        val levelName = when (level) {
            TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE"
            TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW"
            TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL"
            TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN"
            TRIM_MEMORY_BACKGROUND -> "BACKGROUND"
            TRIM_MEMORY_MODERATE -> "MODERATE"
            TRIM_MEMORY_COMPLETE -> "COMPLETE"
            else -> "UNKNOWN($level)"
        }
        
        Timber.d("Trim memory: $levelName")
        
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Timber.d("Running moderate - clearing caches")
                clearMemoryCaches()
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                Timber.d("Running low - aggressive cache clearing")
                clearMemoryCaches()
                clearBitmaps()
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Timber.w("Running critical - clearing everything")
                clearMemoryCaches()
                clearBitmaps()
                clearSensitiveData()
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                Timber.d("UI hidden - releasing UI resources")
                releaseUIResources()
            }
            TRIM_MEMORY_BACKGROUND -> {
                Timber.d("Background - releasing non-critical resources")
                releaseBackgroundResources()
            }
            TRIM_MEMORY_MODERATE -> {
                Timber.d("Moderate - releasing moderate resources")
                releaseModerateResources()
            }
            TRIM_MEMORY_COMPLETE -> {
                Timber.w("Complete - releasing all non-essential resources")
                releaseAllResources()
            }
        }
    }
    
    private fun clearMemoryCaches() {
        try {
            // Clear Glide cache if available
            // Glide.get(this).clearMemory()
            
            // Clear Coil cache if available
            // Coil.ImageLoader(this).evictAll()
            
            Timber.d("Memory caches cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear memory caches")
        }
    }
    
    private fun clearBitmaps() {
        try {
            // Clear bitmap memory
            // Implementation depends on your bitmap caching strategy
            Timber.d("Bitmaps cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear bitmaps")
        }
    }
    
    private fun clearSensitiveData() {
        try {
            val prefs = PreferencesManager(this)
            // Clear temporary sensitive data (keep user preferences)
            prefs.remove("temp_auth_token")
            prefs.remove("temp_session_id")
            Timber.d("Sensitive data cleared")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear sensitive data")
        }
    }
    
    private fun releaseUIResources() {
        try {
            // Release UI-related resources
            Timber.d("UI resources released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release UI resources")
        }
    }
    
    private fun releaseBackgroundResources() {
        try {
            // Release background task resources
            Timber.d("Background resources released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release background resources")
        }
    }
    
    private fun releaseModerateResources() {
        try {
            // Release moderate priority resources
            Timber.d("Moderate resources released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release moderate resources")
        }
    }
    
    private fun releaseAllResources() {
        try {
            // Release all non-essential resources
            clearMemoryCaches()
            Timber.d("All resources released")
        } catch (e: Exception) {
            Timber.e(e, "Failed to release all resources")
        }
    }
    
    override fun getWorkManagerConfiguration(): WorkConfiguration {
        return WorkConfiguration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        val uptime = System.currentTimeMillis() - appStartTime
        Timber.i("🛑 Application terminating - Uptime: ${uptime / 1000}s")
        
        try {
            // Save any pending data
            val prefs = PreferencesManager(this)
            prefs.putLong("last_exit_time", System.currentTimeMillis())
            prefs.putLong("total_uptime", prefs.getLong("total_uptime", 0) + uptime)
            
            // Clean up resources
            cleanup()
        } catch (e: Exception) {
            Timber.e(e, "Error during termination")
        }
        
        Timber.d("Application terminated")
    }
    
    private fun cleanup() {
        try {
            // Cancel all ongoing work
            // WorkManager.getInstance(this).cancelAllWork()
            
            // Release app container resources
            appContainer.cleanup()
            
            // Clear all Timber trees
            Timber.uprootAll()
            
            Timber.d("Cleanup completed")
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
    
    // Utility methods for accessing app state
    fun isAppInForeground(): Boolean = isAppInForeground
    fun getAppUptime(): Long = System.currentTimeMillis() - appStartTime
    fun getActiveActivitiesCount(): Int = activeActivities
}

/**
 * Custom Timber tree for crash reporting
 */
class CrashReportingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == android.util.Log.ERROR) {
            if (t != null) {
                CrashReporting.logException(t)
            } else {
                CrashReporting.logMessage("[$tag] $message")
            }
        }
    }
}

/**
 * Crash reporting utility (implement with your preferred service)
 * Examples: Firebase Crashlytics, Sentry, Bugsnag, etc.
 */
object CrashReporting {
    private var isInitialized = false
    
    fun initialize(context: Context) {
        if (isInitialized) return
        
        try {
            // Example with Firebase Crashlytics:
            // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
            
            isInitialized = true
            Timber.d("CrashReporting initialized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize CrashReporting")
        }
    }
    
    fun logException(throwable: Throwable) {
        if (!isInitialized) return
        
        try {
            // FirebaseCrashlytics.getInstance().recordException(throwable)
            Timber.v("Exception logged: ${throwable.message}")
        } catch (e: Exception) {
            // Ignore logging errors
        }
    }
    
    fun logMessage(message: String) {
        if (!isInitialized) return
        
        try {
            // FirebaseCrashlytics.getInstance().log(message)
            Timber.v("Message logged: $message")
        } catch (e: Exception) {
            // Ignore logging errors
        }
    }
    
    fun setCustomKey(key: String, value: String) {
        if (!isInitialized) return
        
        try {
            // FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            // Ignore
        }
    }
    
    fun setUserId(userId: String) {
        if (!isInitialized) return
        
        try {
            // FirebaseCrashlytics.getInstance().setUserId(userId)
        } catch (e: Exception) {
            // Ignore
        }
    }
}