package com.airmouse.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class SensorCubeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var roll = 0f   // rotation around X (pitch)
    private var pitch = 0f  // rotation around Y (roll)
    private var yaw = 0f    // rotation around Z (azimuth)

    private val paintFace = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A80C0")
        style = Paint.Style.FILL
    }
    private val paintEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private val vertices = arrayOf(
        floatArrayOf(-1f, -1f, -1f), // 0
        floatArrayOf( 1f, -1f, -1f), // 1
        floatArrayOf( 1f, -1f,  1f), // 2
        floatArrayOf(-1f, -1f,  1f), // 3
        floatArrayOf(-1f,  1f, -1f), // 4
        floatArrayOf( 1f,  1f, -1f), // 5
        floatArrayOf( 1f,  1f,  1f), // 6
        floatArrayOf(-1f,  1f,  1f)  // 7
    )

    private val edges = arrayOf(
        intArrayOf(0,1), intArrayOf(1,2), intArrayOf(2,3), intArrayOf(3,0),
        intArrayOf(4,5), intArrayOf(5,6), intArrayOf(6,7), intArrayOf(7,4),
        intArrayOf(0,4), intArrayOf(1,5), intArrayOf(2,6), intArrayOf(3,7)
    )

    private val faces = arrayOf(
        intArrayOf(0,1,2,3), // bottom
        intArrayOf(4,5,6,7), // top
        intArrayOf(0,4,7,3), // left
        intArrayOf(1,5,6,2), // right
        intArrayOf(0,1,5,4), // front
        intArrayOf(3,2,6,7)  // back
    )

    private fun rotateX(p: FloatArray, angleRad: Float): FloatArray {
        val cos = cos(angleRad)
        val sin = sin(angleRad)
        return floatArrayOf(
            p[0],
            p[1] * cos - p[2] * sin,
            p[1] * sin + p[2] * cos
        )
    }

    private fun rotateY(p: FloatArray, angleRad: Float): FloatArray {
        val cos = cos(angleRad)
        val sin = sin(angleRad)
        return floatArrayOf(
            p[0] * cos + p[2] * sin,
            p[1],
            -p[0] * sin + p[2] * cos
        )
    }

    private fun rotateZ(p: FloatArray, angleRad: Float): FloatArray {
        val cos = cos(angleRad)
        val sin = sin(angleRad)
        return floatArrayOf(
            p[0] * cos - p[1] * sin,
            p[0] * sin + p[1] * cos,
            p[2]
        )
    }

    fun updateOrientation(rollRad: Float, pitchRad: Float, yawRad: Float) {
        this.roll = rollRad
        this.pitch = pitchRad
        this.yaw = yawRad
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val scale = (min(width, height) / 2.5f).toFloat()

        val projected = Array(8) { FloatArray(2) }

        for (i in 0 until 8) {
            var p = vertices[i]
            p = rotateX(p, roll)   // roll = rotation around X
            p = rotateY(p, pitch)  // pitch = rotation around Y
            p = rotateZ(p, yaw)    // yaw = rotation around Z
            val z = p[2]
            val factor = 2f / (4f - z)  // simple perspective
            projected[i][0] = cx + p[0] * scale * factor
            projected[i][1] = cy + p[1] * scale * factor
        }

        // Draw faces (back to front)
        for (face in faces) {
            val path = Path()
            path.moveTo(projected[face[0]][0], projected[face[0]][1])
            for (j in 1 until face.size) {
                path.lineTo(projected[face[j]][0], projected[face[j]][1])
            }
            path.close()
            canvas.drawPath(path, paintFace)
        }

        // Draw edges
        for (edge in edges) {
            canvas.drawLine(
                projected[edge[0]][0], projected[edge[0]][1],
                projected[edge[1]][0], projected[edge[1]][1],
                paintEdge
            )
        }
    }
}
