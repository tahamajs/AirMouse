package com.github.mikephil.charting.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BarChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(76, 175, 80)
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(55, 255, 255, 255)
        strokeWidth = 2f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        canvas.drawLine(0f, h - 2f, w, h - 2f, axisPaint)
        val values = floatArrayOf(0.35f, 0.7f, 0.48f, 0.9f, 0.55f)
        val gap = w * 0.04f
        val barWidth = (w - gap * (values.size + 1)) / values.size
        values.forEachIndexed { index, value ->
            val left = gap + index * (barWidth + gap)
            val top = h * (1f - value)
            canvas.drawRoundRect(left, top, left + barWidth, h, 8f, 8f, barPaint)
        }
    }
}
