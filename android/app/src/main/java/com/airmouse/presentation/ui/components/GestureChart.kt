package com.airmouse.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.mikephil.charting.compose.*
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

@Composable
fun GestureChart(
    clicks: Int,
    scrolls: Int,
    rightClicks: Int,
    doubleClicks: Int,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        BarEntry(0f, clicks.toFloat()),
        BarEntry(1f, scrolls.toFloat()),
        BarEntry(2f, rightClicks.toFloat()),
        BarEntry(3f, doubleClicks.toFloat())
    )
    val dataSet = BarDataSet(entries, "Gestures").apply {
        setColors(
            androidx.compose.ui.graphics.Color(0xFFFF5722).toArgb(),
            androidx.compose.ui.graphics.Color(0xFF4CAF50).toArgb(),
            androidx.compose.ui.graphics.Color(0xFF2196F3).toArgb(),
            androidx.compose.ui.graphics.Color(0xFFFFC107).toArgb()
        )
        valueTextSize = 12f
    }
    val barData = BarData(dataSet)
    Column(modifier = modifier) {
        Text("Gesture Statistics", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        BarChart(
            barData,
            modifier = Modifier.fillMaxWidth().height(200.dp),
            description = { },
            xAxis = { position = XAxisPosition.BOTTOM; granularity = 1f; valueFormatter = { index, _ -> listOf("Click", "Scroll", "Right", "Double")[index.toInt()] } }
        )
    }
}