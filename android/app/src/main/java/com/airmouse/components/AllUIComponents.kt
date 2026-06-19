package com.airmouse.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*
import kotlin.random.Random

// ==================== 1. CONTROL DASHBOARD ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlDashboard(
    connectionStatus: String = "Connected",
    activeProfile: String = "Default",
    cursorPosition: Pair<Int, Int> = Pair(500, 500),
    batteryLevel: Int = 85,
    lastGesture: String = "Click",
    onActionClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Control Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid - Passing Modifier.weight(1f) from parent RowScope
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStat(
                    title = "Connection",
                    value = connectionStatus,
                    icon = Icons.Default.Wifi,
                    color = if (connectionStatus == "Connected") Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                DashboardStat(
                    title = "Profile",
                    value = activeProfile,
                    icon = Icons.Default.Person,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DashboardStat(
                    title = "Battery",
                    value = "$batteryLevel%",
                    icon = Icons.Default.BatteryFull,
                    color = when {
                        batteryLevel > 50 -> Color(0xFF4CAF50)
                        batteryLevel > 20 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cursor Position
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cursor Position:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "X: ${cursorPosition.first} | Y: ${cursorPosition.second}",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Last Gesture
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Last Gesture:", style = MaterialTheme.typography.bodyMedium)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        lastGesture,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Action Buttons - Passing Modifier.weight(1f) from parent RowScope
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton("Calibrate", Icons.Default.Tune, modifier = Modifier.weight(1f)) { onActionClick("calibrate") }
                QuickActionButton("Settings", Icons.Default.Settings, modifier = Modifier.weight(1f)) { onActionClick("settings") }
                QuickActionButton("Statistics", Icons.Default.Assessment, modifier = Modifier.weight(1f)) { onActionClick("stats") }
                QuickActionButton("Help", Icons.AutoMirrored.Filled.Help, modifier = Modifier.weight(1f)) { onActionClick("help") }
            }
        }
    }
}

@Composable
fun DashboardStat(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 11.sp)
    }
}

// ==================== 2. GESTURE TRAINING CENTER ====================

data class GestureTrainingItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val progress: Int,
    val successRate: Int,
    val attempts: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureTrainingCenter(
    onTrainGesture: (String) -> Unit = {}
) {
    val gestures = listOf(
        GestureTrainingItem("Click", Icons.Default.PlayArrow, 100, 95, 150),
        GestureTrainingItem("Double Click", Icons.Default.Repeat, 85, 88, 80),
        GestureTrainingItem("Right Click", Icons.Default.Menu, 70, 75, 60),
        GestureTrainingItem("Scroll Up", Icons.Default.ArrowUpward, 90, 92, 45),
        GestureTrainingItem("Scroll Down", Icons.Default.ArrowDownward, 88, 90, 42),
        GestureTrainingItem("Swipe Left", Icons.AutoMirrored.Filled.ArrowBack, 60, 65, 30),
        GestureTrainingItem("Swipe Right", Icons.AutoMirrored.Filled.ArrowForward, 55, 60, 28),
        GestureTrainingItem("Circle CW", Icons.Default.Autorenew, 40, 50, 20)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Gesture Training Center",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Train your gestures for better accuracy",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            gestures.forEach { gesture ->
                GestureTrainingCard(gesture, onTrainGesture)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun GestureTrainingCard(gesture: GestureTrainingItem, onTrain: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(gesture.icon, contentDescription = gesture.name, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(gesture.name, fontWeight = FontWeight.Bold)
                        Text(
                            "Attempts: ${gesture.attempts} | Success: ${gesture.successRate}%",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Button(onClick = { onTrain(gesture.name) }) {
                    Text("Train")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { gesture.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    gesture.progress >= 80 -> Color(0xFF4CAF50)
                    gesture.progress >= 50 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Mastery: ${gesture.progress}% - ${getMasteryLevel(gesture.progress)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getMasteryLevel(progress: Int): String = when {
    progress >= 90 -> "Expert"
    progress >= 70 -> "Advanced"
    progress >= 50 -> "Intermediate"
    progress >= 30 -> "Beginner"
    else -> "Novice"
}

// ==================== 3. MACRO RECORDER ====================

data class MacroAction(
    val id: String,
    val type: String,
    val description: String,
    val delay: Long = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroRecorder(
    onRecord: () -> Unit = {},
    onSave: (String) -> Unit = {},
    onPlay: (String) -> Unit = {}
) {
    var isRecording by remember { mutableStateOf(false) }
    var macroName by remember { mutableStateOf("") }
    var recordedActions by remember { mutableStateOf(listOf<MacroAction>()) }
    var recordingTime by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Macro Recorder",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Recording controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = macroName,
                    onValueChange = { macroName = it },
                    label = { Text("Macro Name") },
                    modifier = Modifier.weight(1f),
                    enabled = !isRecording
                )

                Button(
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                            onSave(macroName)
                        } else {
                            recordedActions = emptyList()
                            isRecording = true
                            onRecord()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (isRecording) "Stop" else "Record"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRecording) "Stop Recording" else "Start Recording")
                }
            }

            if (isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Recording... ${formatTime(recordingTime)}", color = Color.Red)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Recorded actions
            if (recordedActions.isNotEmpty()) {
                Text("Recorded Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                recordedActions.forEach { action ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("• ${action.type}: ${action.description}", fontSize = 12.sp)
                        if (action.delay > 0) {
                            Text("delay: ${action.delay}ms", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Text("Add Actions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { /* Add click action */ }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Click", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Click")
                }
                OutlinedButton(onClick = { /* Add scroll action */ }) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Scroll", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scroll")
                }
                OutlinedButton(onClick = { /* Add delay action */ }) {
                    Icon(Icons.Default.Timer, contentDescription = "Delay", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delay")
                }
            }
        }
    }
}

private fun formatTime(seconds: Int): String {
    return String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}

// ==================== 4. GAME PROFILES MANAGER ====================

data class GameProfile(
    val id: String,
    val name: String,
    val icon: String,
    val sensitivity: Float,
    val aimAssist: Boolean,
    val rapidFire: Boolean,
    val lastPlayed: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameProfilesManager(
    onProfileSelect: (GameProfile) -> Unit = {},
    onEditProfile: (GameProfile) -> Unit = {},
    onDeleteProfile: (GameProfile) -> Unit = {}
) {
    val profiles = listOf(
        GameProfile("1", "Call of Duty", "🎮", 1.2f, true, true, "2 min ago"),
        GameProfile("2", "PUBG", "🔫", 0.9f, true, false, "1 hour ago"),
        GameProfile("3", "Genshin Impact", "🗡️", 0.8f, false, false, "Yesterday"),
        GameProfile("4", "Valorant", "🎯", 1.1f, true, true, "3 days ago")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Game Profiles",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = { /* Add new profile */ }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            profiles.forEach { profile ->
                GameProfileCard(profile, onProfileSelect, onEditProfile, onDeleteProfile)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun GameProfileCard(
    profile: GameProfile,
    onSelect: (GameProfile) -> Unit,
    onEdit: (GameProfile) -> Unit,
    onDelete: (GameProfile) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(profile) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(profile.icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(profile.name, fontWeight = FontWeight.Bold)
                    Text(
                        "Sensitivity: ${profile.sensitivity}x | Last played: ${profile.lastPlayed}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        if (profile.aimAssist) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF4CAF50).copy(alpha = 0.2f)) {
                                Text("Aim Assist", fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        if (profile.rapidFire) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFF44336).copy(alpha = 0.2f)) {
                                Text("Rapid Fire", fontSize = 8.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
            Row {
                IconButton(onClick = { onEdit(profile) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onDelete(profile) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ==================== 5. ANALYTICS DASHBOARD ====================

data class AnalyticsData(
    val date: String,
    val clicks: Int,
    val gestures: Int,
    val distance: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboard() {
    val analyticsData = listOf(
        AnalyticsData("Mon", 45, 120, 150f),
        AnalyticsData("Tue", 52, 135, 180f),
        AnalyticsData("Wed", 38, 98, 120f),
        AnalyticsData("Thu", 61, 145, 200f),
        AnalyticsData("Fri", 49, 128, 170f),
        AnalyticsData("Sat", 35, 89, 100f),
        AnalyticsData("Sun", 28, 76, 80f)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Analytics Dashboard",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Heatmap
            Text("Gesture Heatmap", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            GestureHeatmap()

            Spacer(modifier = Modifier.height(16.dp))

            // Activity Chart
            Text("Activity Timeline", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            ActivityChart(analyticsData)

            Spacer(modifier = Modifier.height(16.dp))

            // Stats summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatSummary("Total Clicks", analyticsData.map { it.clicks }.sum().toString(), Icons.Default.PlayArrow)
                StatSummary("Total Gestures", analyticsData.map { it.gestures }.sum().toString(), Icons.Default.Gesture)
                StatSummary("Distance", "${analyticsData.map { it.distance }.sum().toInt()} units", Icons.Default.Map)
            }
        }
    }
}

@Composable
fun GestureHeatmap() {
    val gestures = listOf("Click", "Double", "Right", "Scroll", "Swipe L", "Swipe R", "Circle")
    val intensities = listOf(95, 70, 60, 85, 45, 40, 30)

    Column {
        gestures.forEachIndexed { index, gesture ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(gesture, fontSize = 11.sp, modifier = Modifier.width(60.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Color(0xFFFF5722).copy(alpha = intensities[index] / 100f)
                        )
                )
                Text("${intensities[index]}%", fontSize = 10.sp, modifier = Modifier.width(35.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun ActivityChart(data: List<AnalyticsData>) {
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1)
        val maxValue = data.maxOfOrNull { it.gestures } ?: 1

        val points = data.mapIndexed { i, d ->
            Offset(
                x = i * stepX,
                y = height - (d.gestures.toFloat() / maxValue) * height
            )
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFFFF5722),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 3f
            )
        }

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(width, height)
            close()
        }
        drawPath(path, Color(0xFFFF5722).copy(alpha = 0.2f))
    }
}

@Composable
fun StatSummary(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== 6. PLUGIN/EXTENSION STORE ====================

data class Extension(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val rating: Float,
    val downloads: Int,
    val category: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionStore(
    onInstall: (Extension) -> Unit = {}
) {
    val extensions = listOf(
        Extension("1", "Pro Gestures Pack", "50+ custom gestures", "$4.99", 4.8f, 1250, "Gestures"),
        Extension("2", "Dark Theme Ultimate", "AMOLED optimized theme", "$2.99", 4.9f, 890, "Themes"),
        Extension("3", "Voice Commands Pro", "Advanced voice control", "$6.99", 4.7f, 560, "Voice"),
        Extension("4", "Gaming Mode+", "Enhanced gaming features", "$3.99", 4.6f, 2100, "Gaming")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Extension Store",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Discover new features and content",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Gestures", "Themes", "Voice", "Gaming")) { category ->
                    FilterChip(
                        selected = false,
                        onClick = { /* Filter by category */ },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            extensions.forEach { extension ->
                ExtensionCard(extension, onInstall)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ExtensionCard(extension: Extension, onInstall: (Extension) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(extension.name, fontWeight = FontWeight.Bold)
                Text(extension.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("★ ${extension.rating}", fontSize = 10.sp, color = Color(0xFFFFC107))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${extension.downloads} downloads", fontSize = 10.sp)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(extension.price, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Button(onClick = { onInstall(extension) }, modifier = Modifier.height(32.dp)) {
                    Text("Install", fontSize = 11.sp)
                }
            }
        }
    }
}

// ==================== 7. COMMUNITY HUB ====================

data class CommunityPost(
    val id: String,
    val author: String,
    val title: String,
    val content: String,
    val likes: Int,
    val comments: Int,
    val timestamp: String,
    val isLiked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHub(
    onLike: (String) -> Unit = {},
    onComment: (String) -> Unit = {},
    onShare: (String) -> Unit = {}
) {
    val posts = listOf(
        CommunityPost("1", "GestureMaster", "My custom gesture setup", "Created a perfect circle gesture for volume control...", 45, 12, "2 hours ago"),
        CommunityPost("2", "AirMousePro", "Pro tip: Calibration guide", "Here's how I achieved perfect cursor accuracy...", 89, 23, "5 hours ago"),
        CommunityPost("3", "Gamer123", "Best sensitivity for FPS games", "After months of testing, here are my recommendations...", 67, 18, "1 day ago")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Community Hub",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Search community...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            posts.forEach { post ->
                CommunityPostCard(post, onLike, onComment, onShare)
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Load More")
            }
        }
    }
}

@Composable
fun CommunityPostCard(
    post: CommunityPost,
    onLike: (String) -> Unit,
    onComment: (String) -> Unit,
    onShare: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(post.author, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(post.timestamp, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(post.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(post.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(onClick = { onLike(post.id) }) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = "Like",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(post.likes.toString(), fontSize = 11.sp)
                }
                TextButton(onClick = { onComment(post.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(post.comments.toString(), fontSize = 11.sp)
                }
                IconButton(onClick = { onShare(post.id) }) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
