package com.airmouse.calibration

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class MagAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val path = Path()
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555")
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F51B5")
    }
    private val pathMeasure = PathMeasure()
    private var progress = 0f
    private var animator: ValueAnimator? = null

    init {
        val width = 300f
        val height = 300f
        path.moveTo(0f, 0f)
        for (i in 0..360 step 5) {
            val t = Math.toRadians(i.toDouble())
            val x = (width * sin(t)).toFloat()
            val y = (height * sin(2 * t) * 0.6f).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        pathMeasure.setPath(path, false)

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(0.5f, 0.5f)

        canvas.drawPath(path, pathPaint)

        val point = FloatArray(2)
        pathMeasure.getPosTan(pathMeasure.length * progress, point, null)
        canvas.drawCircle(point[0], point[1], 20f, phonePaint)

        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}