package com.airmouse.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AnimatedToast(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    type: ToastType = ToastType.INFO,
    duration: Long = 3000
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(duration)
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (type) {
                    ToastType.SUCCESS -> Color(0xFF4CAF50)
                    ToastType.ERROR -> Color(0xFFF44336)
                    ToastType.WARNING -> Color(0xFFFF9800)
                    ToastType.INFO -> MaterialTheme.colorScheme.primary
                }
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    when (type) {
                        ToastType.SUCCESS -> Icons.Default.CheckCircle
                        ToastType.ERROR -> Icons.Default.Error
                        ToastType.WARNING -> Icons.Default.Warning
                        ToastType.INFO -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    message,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}
