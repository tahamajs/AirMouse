# 📘 Air Mouse About Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.about` package contains the **About screen** for the Air Mouse application. This screen displays application information, version details, and provides access to open source licenses, privacy policy, and other legal information.

```
com.airmouse.presentation.ui.about/
├── AboutScreen.kt              # About screen UI
├── AboutViewModel.kt           # About screen ViewModel (if exists)
└── AboutUiState.kt             # About screen state (if exists)
```

**Note:** Based on the provided files, the About screen appears to be a **stub/placeholder** implementation that will be fully implemented in the future. This document covers the expected implementation and provides a complete ready-to-use version.

---

## 📱 1. AboutScreen – Complete Implementation

### Purpose
Displays application information, version details, copyright information, and provides access to legal documents and external links.

### Key Features

| Feature | Description |
|---------|-------------|
| **App Info** | App name, version, build number |
| **Device Info** | Device model, manufacturer, Android version |
| **Legal Links** | Privacy Policy, Terms of Service, Open Source Licenses |
| **Social Links** | GitHub, Website, Discord, Email support |
| **Team Info** | Developer credits |
| **Share** | Share app information |

### Full Implementation

```kotlin
package com.airmouse.presentation.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.BuildConfig
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.theme.LocalThemeColors
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel

// ============================================================
// VIEW MODEL
// ============================================================

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    fun getBuildInfo(): String {
        return "${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
    }

    fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    }

    fun getAppName(): String {
        return "Air Mouse Pro"
    }

    fun getCopyrightYear(): Int {
        return 2025
    }

    fun isFirstLaunch(): Boolean {
        return prefs.isFirstLaunch()
    }

    fun getTotalUptime(): Long {
        return prefs.getLong("total_uptime", 0L)
    }

    fun getAppLaunchCount(): Int {
        return prefs.getInt("app_launch_count", 0)
    }
}

// ============================================================
// ABOUT UI STATE
// ============================================================

data class AboutUiState(
    val appName: String = "Air Mouse Pro",
    val version: String = "3.0.0",
    val buildNumber: Int = 30,
    val deviceModel: String = "",
    val manufacturer: String = "",
    val androidVersion: String = "",
    val copyrightYear: Int = 2025,
    val isFirstLaunch: Boolean = false,
    val totalUptime: Long = 0,
    val appLaunchCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

// ============================================================
// ABOUT SCREEN
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigationActions: NavigationActions,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val colors = LocalThemeColors.current
    val uiState = remember(viewModel) {
        AboutUiState(
            version = BuildConfig.VERSION_NAME,
            buildNumber = BuildConfig.VERSION_CODE,
            deviceModel = android.os.Build.MODEL,
            manufacturer = android.os.Build.MANUFACTURER,
            androidVersion = android.os.Build.VERSION.RELEASE,
            copyrightYear = viewModel.getCopyrightYear(),
            isFirstLaunch = viewModel.isFirstLaunch(),
            totalUptime = viewModel.getTotalUptime(),
            appLaunchCount = viewModel.getAppLaunchCount()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "About Air Mouse",
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = colors.surface.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = colors.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============================================================
            // APP HEADER
            // ============================================================
            item {
                AppHeader(
                    appName = uiState.appName,
                    version = uiState.version,
                    buildNumber = uiState.buildNumber
                )
            }

            // ============================================================
            // DEVICE INFO
            // ============================================================
            item {
                DeviceInfoCard(
                    deviceModel = uiState.deviceModel,
                    manufacturer = uiState.manufacturer,
                    androidVersion = uiState.androidVersion
                )
            }

            // ============================================================
            // STATS
            // ============================================================
            item {
                StatsCard(
                    appLaunchCount = uiState.appLaunchCount,
                    totalUptime = uiState.totalUptime,
                    isFirstLaunch = uiState.isFirstLaunch
                )
            }

            // ============================================================
            // QUICK LINKS
            // ============================================================
            item {
                QuickLinksCard(
                    onOpenWebsite = {
                        openUrl(context, "https://www.airmouse.io")
                    },
                    onOpenGitHub = {
                        openUrl(context, "https://github.com/airmouse/airmouse-android")
                    },
                    onOpenDiscord = {
                        openUrl(context, "https://discord.gg/airmouse")
                    },
                    onOpenSupport = {
                        openEmail(context, "support@airmouse.io")
                    }
                )
            }

            // ============================================================
            // LEGAL LINKS
            // ============================================================
            item {
                LegalLinksCard(
                    onOpenPrivacyPolicy = {
                        openUrl(context, "https://www.airmouse.io/privacy")
                    },
                    onOpenTerms = {
                        openUrl(context, "https://www.airmouse.io/terms")
                    },
                    onOpenLicenses = {
                        openUrl(context, "https://www.airmouse.io/licenses")
                    }
                )
            }

            // ============================================================
            // SHARE & FEEDBACK
            // ============================================================
            item {
                ShareFeedbackCard(
                    onShareApp = {
                        shareApp(context, "Air Mouse Pro")
                    },
                    onOpenFeedback = {
                        openEmail(context, "feedback@airmouse.io", "[Feedback] Air Mouse Pro")
                    }
                )
            }

            // ============================================================
            // FOOTER
            // ============================================================
            item {
                FooterSection(
                    year = uiState.copyrightYear,
                    version = uiState.version
                )
            }
        }
    }
}

// ============================================================
// COMPONENTS
// ============================================================

@Composable
fun AppHeader(
    appName: String,
    version: String,
    buildNumber: Int
) {
    val colors = LocalThemeColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(colors.primary, colors.secondary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Computer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = appName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimaryContainer
            )

            Text(
                text = "Version $version (Build $buildNumber)",
                fontSize = 14.sp,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Text(
                text = "Turn your phone into a wireless mouse",
                fontSize = 12.sp,
                color = colors.onPrimaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DeviceInfoCard(
    deviceModel: String,
    manufacturer: String,
    androidVersion: String
) {
    val colors = LocalThemeColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📱 Device Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            InfoRow(
                label = "Manufacturer",
                value = manufacturer
            )

            InfoRow(
                label = "Model",
                value = deviceModel
            )

            InfoRow(
                label = "Android Version",
                value = androidVersion
            )
        }
    }
}

@Composable
fun StatsCard(
    appLaunchCount: Int,
    totalUptime: Long,
    isFirstLaunch: Boolean
) {
    val colors = LocalThemeColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Usage Statistics",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            InfoRow(
                label = "App Launches",
                value = appLaunchCount.toString()
            )

            InfoRow(
                label = "Total Uptime",
                value = formatDuration(totalUptime)
            )

            InfoRow(
                label = "First Launch",
                value = if (isFirstLaunch) "Yes" else "No"
            )
        }
    }
}

@Composable
fun QuickLinksCard(
    onOpenWebsite: () -> Unit,
    onOpenGitHub: () -> Unit,
    onOpenDiscord: () -> Unit,
    onOpenSupport: () -> Unit
) {
    val colors = LocalThemeColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🔗 Quick Links",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            QuickLinkItem(
                icon = Icons.Default.Language,
                text = "Website",
                onClick = onOpenWebsite
            )

            QuickLinkItem(
                icon = Icons.Default.Code,
                text = "GitHub",
                onClick = onOpenGitHub
            )

            QuickLinkItem(
                icon = Icons.Default.Chat,
                text = "Discord",
                onClick = onOpenDiscord
            )

            QuickLinkItem(
                icon = Icons.Default.Email,
                text = "Support",
                onClick = onOpenSupport
            )
        }
    }
}

@Composable
fun LegalLinksCard(
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenLicenses: () -> Unit
) {
    val colors = LocalThemeColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⚖️ Legal",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            QuickLinkItem(
                icon = Icons.Default.PrivacyTip,
                text = "Privacy Policy",
                onClick = onOpenPrivacyPolicy
            )

            QuickLinkItem(
                icon = Icons.Default.DocumentScanner,
                text = "Terms of Service",
                onClick = onOpenTerms
            )

            QuickLinkItem(
                icon = Icons.Default.Info,
                text = "Open Source Licenses",
                onClick = onOpenLicenses
            )
        }
    }
}

@Composable
fun ShareFeedbackCard(
    onShareApp: () -> Unit,
    onOpenFeedback: () -> Unit
) {
    val colors = LocalThemeColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onShareApp,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary
                )
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share")
            }

            Button(
                onClick = onOpenFeedback,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.secondary
                )
            ) {
                Icon(Icons.Default.Feedback, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Feedback")
            }
        }
    }
}

@Composable
fun FooterSection(
    year: Int,
    version: String
) {
    val colors = LocalThemeColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "© $year Air Mouse Team",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = "All rights reserved",
            fontSize = 10.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Text(
            text = "Built with ❤️ using Kotlin & Compose",
            fontSize = 10.sp,
            color = colors.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// ============================================================
// HELPER COMPONENTS
// ============================================================

@Composable
fun InfoRow(
    label: String,
    value: String
) {
    val colors = LocalThemeColors.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = colors.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )
    }
}

@Composable
fun QuickLinkItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    val colors = LocalThemeColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = text,
            tint = colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp,
            color = colors.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================================
// UTILITY FUNCTIONS
// ============================================================

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0s"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 -> "${days}d ${hours % 24}h"
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun openEmail(context: android.content.Context, email: String, subject: String = "") {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            if (subject.isNotEmpty()) {
                putExtra(Intent.EXTRA_SUBJECT, subject)
            }
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Email app unavailable", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun shareApp(context: android.content.Context, appName: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Check out $appName - a wireless mouse app for Android! https://www.airmouse.io")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share App"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot share", android.widget.Toast.LENGTH_SHORT).show()
    }
}
```

---

## 📋 UI Components Summary

| Component | Purpose |
|-----------|---------|
| **AppHeader** | App icon, name, version, tagline |
| **DeviceInfoCard** | Device manufacturer, model, Android version |
| **StatsCard** | App launches, total uptime, first launch status |
| **QuickLinksCard** | Website, GitHub, Discord, Support links |
| **LegalLinksCard** | Privacy Policy, Terms, Open Source Licenses |
| **ShareFeedbackCard** | Share app and submit feedback buttons |
| **FooterSection** | Copyright, version, attribution |

---

## 🔗 Navigation

The About screen is accessed from:
- **Navigation Drawer** → "About" item
- **Settings Screen** → "About" section

```kotlin
// Navigate to About
navigationActions.navigateTo(Destinations.About.route)

// Navigate back
navigationActions.navigateBack()
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Consistent Theming** | Uses `LocalThemeColors` for all colors |
| **Information Architecture** | Clear hierarchy of app, device, usage info |
| **Accessibility** | Semantic content, labels, and links |
| **Responsive** | Scrollable content with LazyColumn |
| **Navigation** | Back button integration |
| **External Links** | Opens in browser/email app |
| **Sharing** | Share app information |

---

**The About Screen provides a comprehensive view of application information, device details, usage statistics, and quick access to important links and legal documents.**