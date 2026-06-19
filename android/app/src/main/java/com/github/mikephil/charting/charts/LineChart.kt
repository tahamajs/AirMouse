package com.github.mikephil.charting.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class LineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(33, 150, 243)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(45, 255, 255, 255)
        strokeWidth = 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        repeat(4) { index ->
            val y = h * (index + 1) / 5f
            canvas.drawLine(0f, y, w, y, gridPaint)
        }

        val points = floatArrayOf(0.80f, 0.55f, 0.62f, 0.38f, 0.44f, 0.28f, 0.36f)
        var lastX = 0f
        var lastY = h * points.first()
        points.forEachIndexed { index, value ->
            val x = w * index / (points.lastIndex.coerceAtLeast(1))
            val y = h * value
            if (index > 0) canvas.drawLine(lastX, lastY, x, y, linePaint)
            lastX = x
            lastY = y
        }
    }
}
