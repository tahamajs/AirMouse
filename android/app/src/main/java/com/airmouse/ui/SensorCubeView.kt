package com.airmouse.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class SensorCubeView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.rgb(0, 150, 136)
        style = Paint.Style.FILL_AND_STROKE
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Simple visualization for sensor data
        canvas.drawColor(Color.parseColor("#1a1a2e"))
        // Draw a cube representation
        val cx = width / 2f
        val cy = height / 2f
        val size = minOf(width, height) / 3f
        
        paint.color = Color.rgb(0, 150, 136)
        canvas.drawRect(cx - size/2, cy - size/2, cx + size/2, cy + size/2, paint)
        canvas.drawRect(cx - size/3, cy - size/3, cx + size/3, cy + size/3, paint)
        canvas.drawLine(cx - size/2, cy - size/2, cx - size/3, cy - size/3, paint)
        canvas.drawLine(cx + size/2, cy - size/2, cx + size/3, cy - size/3, paint)
        canvas.drawLine(cx - size/2, cy + size/2, cx - size/3, cy + size/3, paint)
        canvas.drawLine(cx + size/2, cy + size/2, cx + size/3, cy + size/3, paint)
    }
    
    fun updateOrientation(roll: Float, pitch: Float, yaw: Float) {
        // Update rotation if needed
        invalidate()
    }
}
