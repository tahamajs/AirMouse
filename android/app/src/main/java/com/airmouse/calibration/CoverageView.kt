package com.airmouse.calibration

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class CoverageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F51B5")
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80CBC4")
        style = Paint.Style.FILL
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF9800")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val path = Path()
    private var animProgress = 0f
    private var coveragePercent = 0f
    private var animator: ValueAnimator? = null

    init {
        val w = 400f
        val h = 300f
        // Create a figure‑8 (lemniscate) path
        for (t in 0..360 step 5) {
            val rad = Math.toRadians(t.toDouble())
            val x = (w * 0.4 * sin(rad)).toFloat()
            val y = (h * 0.3 * sin(2 * rad)).toFloat()
            if (t == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
    }

    fun startAnimation() {
        if (animator == null || !animator!!.isRunning) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 4000
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener {
                    animProgress = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    fun updateCoverage(percent: Int) {
        coveragePercent = percent / 100f
        invalidate()
    }

    fun reset() {
        coveragePercent = 0f
        animProgress = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(0.8f, 0.8f)

        // Draw the figure‑8 path
        canvas.drawPath(path, paint)

        // Draw moving dot along the path
        val measure = PathMeasure(path, false)
        if (measure.length > 0) {
            val pos = FloatArray(2)
            measure.getPosTan(measure.length * animProgress, pos, null)
            canvas.drawCircle(pos[0], pos[1], 16f, fillPaint)
        }

        // Draw coverage ring (grows with progress)
        val radius = 80f + 40f * coveragePercent
        canvas.drawCircle(0f, 0f, radius, ringPaint)

        canvas.restore()
    }
}