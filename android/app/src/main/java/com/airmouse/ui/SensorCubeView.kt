package com.airmouse.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SensorCubeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(26, 29, 36)
        style = Paint.Style.FILL
    }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 188, 212)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val size = minOf(w, h) * 0.45f
        val left = (w - size) / 2f
        val top = (h - size) / 2f
        canvas.drawRoundRect(left, top, left + size, top + size, 18f, 18f, facePaint)
        canvas.drawRoundRect(left, top, left + size, top + size, 18f, 18f, edgePaint)
        canvas.drawLine(left, top, left + size * 0.35f, top - size * 0.20f, edgePaint)
        canvas.drawLine(left + size, top, left + size * 1.35f, top - size * 0.20f, edgePaint)
        canvas.drawLine(left + size * 1.35f, top - size * 0.20f, left + size * 1.35f, top + size * 0.80f, edgePaint)
        canvas.drawLine(left + size, top + size, left + size * 1.35f, top + size * 0.80f, edgePaint)
    }
}
