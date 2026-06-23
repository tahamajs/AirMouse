package com.airmouse.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TypewriterText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    delayMs: Long = 50,
    cursorVisible: Boolean = true
) {
    var displayedText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    
    LaunchedEffect(text) {
        displayedText = ""
        for (i in text.indices) {
            delay(delayMs)
            displayedText = text.substring(0, i + 1)
        }
    }
    
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }
    
    Row(modifier = modifier) {
        Text(
            text = displayedText,
            style = style
        )
        if (cursorVisible && displayedText.length < text.length) {
            Text(
                text = if (showCursor) "|" else " ",
                style = style,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}