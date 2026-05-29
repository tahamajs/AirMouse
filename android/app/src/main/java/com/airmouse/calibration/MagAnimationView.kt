package com.airmouse.calibration

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.math.cos

class MagAnimationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#3F51B5") }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private var animatedFraction = 0f
    private var animator: ValueAnimator? = null

    init {
        // Build a figure‑8 path (Lissajous curve)
        val w = 300f; val h = 300f
        path.moveTo(0f, 0f)
        for (i in 0..360 step 5) {
            val t = Math.toRadians(i.toDouble())
            val x = (w * sin(t)).toFloat()
            val y = (h * sin(2 * t) * 0.6).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        // Start looping animation
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                animatedFraction = animation.animatedFraction
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        // Center the path in the view
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(0.5f, 0.5f)   // adjust size

        // Draw the faded path
        pathPaint.alpha = 60
        canvas.drawPath(path, pathPaint)

        // Calculate point on path at fraction
        val pm = PathMeasure(path, false)
        val point = FloatArray(2)
        pm.getPosTan(pm.length * animatedFraction, point, null)

        // Draw a small phone icon at that point
        canvas.drawCircle(point[0], point[1], 20f, phonePaint)  // placeholder circle; replace with phone bitmap

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}