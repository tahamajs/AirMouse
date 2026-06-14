package com.airmouse.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.*
import android.widget.TextView
import com.airmouse.R
import com.airmouse.sensors.SensorService
import kotlinx.coroutines.*
import kotlin.math.abs

/**
 * Debug overlay for real-time sensor visualization
 * Shows gyroscope, accelerometer, orientation data with live graphs
 */
class DebugOverlay(private val context: Context) {

    companion object {
        private const val UPDATE_INTERVAL_MS = 50L
        private const val GRAPH_DURATION_MS = 5000L
        private const val GRAPH_HEIGHT = 100
        private const val GRAPH_WIDTH = 300
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var isVisible = false

    private var sensorService: SensorService? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null

    // Data storage for graphs
    private val gyroHistory = mutableListOf<Float>()
    private val accelHistory = mutableListOf<Float>()
    private val orientationHistory = mutableListOf<Float>()
    private var lastTimestamp = 0L

    // UI elements
    private lateinit var gyroTextView: TextView
    private lateinit var accelTextView: TextView
    private lateinit var orientationTextView: TextView
    private lateinit var gestureTextView: TextView
    private lateinit var connectionTextView: TextView
    private lateinit var fpsTextView: TextView
    private lateinit var graphCanvas: GraphCanvas

    /**
     * Initialize the debug overlay
     */
    fun initialize() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END
        params.x = 16
        params.y = 100

        overlayView = LayoutInflater.from(context).inflate(R.layout.debug_overlay, null)

        gyroTextView = overlayView!!.findViewById(R.id.debug_gyro)
        accelTextView = overlayView!!.findViewById(R.id.debug_accel)
        orientationTextView = overlayView!!.findViewById(R.id.debug_orientation)
        gestureTextView = overlayView!!.findViewById(R.id.debug_gesture)
        connectionTextView = overlayView!!.findViewById(R.id.debug_connection)
        fpsTextView = overlayView!!.findViewById(R.id.debug_fps)
        graphCanvas = overlayView!!.findViewById(R.id.debug_graph)

        // Make overlay draggable
        setupDraggable(params)

        windowManager?.addView(overlayView, params)
    }

    private fun setupDraggable(params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Set sensor service to monitor
     */
    fun setSensorService(service: SensorService) {
        sensorService = service
        startMonitoring()
    }

    private fun startMonitoring() {
        updateJob?.cancel()
        updateJob = scope.launch {
            var lastFrameTime = System.currentTimeMillis()
            var frameCount = 0

            while (isVisible) {
                updateSensorDisplay()
                updateGraphs()

                // Calculate FPS
                frameCount++
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFrameTime >= 1000) {
                    val fps = frameCount
                    fpsTextView.text = "FPS: $fps"
                    frameCount = 0
                    lastFrameTime = currentTime
                }

                delay(UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun updateSensorDisplay() {
        val service = sensorService ?: return

        val gyro = service.getRawGyro()
        gyroTextView.text = String.format("Gyro: X:%.2f Y:%.2f Z:%.2f", gyro.first, gyro.second, gyro.third)

        val accel = service.getRawAccel()
        accelTextView.text = String.format("Accel: X:%.2f Y:%.2f Z:%.2f", accel.first, accel.second, accel.third)

        val orientation = service.getCurrentOrientation()
        orientationTextView.text = String.format("Roll:%.1f° Yaw:%.1f°", orientation.first, orientation.second)

        val status = if (service.isCalibrated()) "Calibrated" else "Not Calibrated"
        gestureTextView.text = "Status: $status"

        connectionTextView.text = if (service.isStable()) "Stable" else "Moving"

        // Add to history
        gyroHistory.add(gyro.second)  // Use gyro Y for click detection
        accelHistory.add(accel.second) // Use accel Y for scroll
        orientationHistory.add(orientation.first) // Use roll

        // Limit history size
        val maxHistory = (GRAPH_DURATION_MS / UPDATE_INTERVAL_MS).toInt()
        while (gyroHistory.size > maxHistory) gyroHistory.removeAt(0)
        while (accelHistory.size > maxHistory) accelHistory.removeAt(0)
        while (orientationHistory.size > maxHistory) orientationHistory.removeAt(0)
    }

    private fun updateGraphs() {
        graphCanvas.setData(gyroHistory, accelHistory, orientationHistory)
        graphCanvas.invalidate()
    }

    /**
     * Show debug overlay
     */
    fun show() {
        if (isVisible) return
        isVisible = true
        overlayView?.visibility = View.VISIBLE
        startMonitoring()
    }

    /**
     * Hide debug overlay
     */
    fun hide() {
        if (!isVisible) return
        isVisible = false
        overlayView?.visibility = View.GONE
        updateJob?.cancel()
    }

    /**
     * Toggle debug overlay visibility
     */
    fun toggle() {
        if (isVisible) hide() else show()
    }

    /**
     * Check if overlay is visible
     */
    fun isVisible(): Boolean = isVisible

    /**
     * Clean up resources
     */
    fun destroy() {
        hide()
        updateJob?.cancel()
        scope.cancel()
        try {
            windowManager?.removeView(overlayView)
        } catch (e: Exception) {
            // View already removed
        }
        overlayView = null
    }

    /**
     * Custom view for real-time graphs
     */
    inner class GraphCanvas(context: Context) : View(context) {
        private var gyroData = listOf<Float>()
        private var accelData = listOf<Float>()
        private var orientationData = listOf<Float>()

        private val paintGyro = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 87, 34)  // Deep Orange
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        private val paintAccel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(33, 150, 243)  // Blue
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        private val paintOrientation = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(76, 175, 80)   // Green
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        private val paintGrid = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 255, 255, 255)
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        init {
            layoutParams = ViewGroup.LayoutParams(GRAPH_WIDTH, GRAPH_HEIGHT)
        }

        fun setData(gyro: List<Float>, accel: List<Float>, orientation: List<Float>) {
            gyroData = gyro
            accelData = accel
            orientationData = orientation
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val width = width.toFloat()
            val height = height.toFloat()

            // Draw grid
            for (i in 0..4) {
                val y = height * i / 4
                canvas.drawLine(0f, y, width, y, paintGrid)
            }
            canvas.drawLine(width / 2, 0f, width / 2, height, paintGrid)

            // Draw graphs
            drawGraph(canvas, gyroData, paintGyro, -30f, 30f)
            drawGraph(canvas, accelData, paintAccel, -20f, 20f)
            drawGraph(canvas, orientationData, paintOrientation, -90f, 90f)
        }

        private fun drawGraph(canvas: Canvas, data: List<Float>, paint: Paint, min: Float, max: Float) {
            if (data.size < 2) return

            val width = width.toFloat()
            val height = height.toFloat()
            val range = max - min

            val path = Path()
            for (i in 0 until data.size - 1) {
                val x1 = i * width / data.size
                val x2 = (i + 1) * width / data.size

                val y1 = height - ((data[i] - min) / range) * height
                val y2 = height - ((data[i + 1] - min) / range) * height

                if (i == 0) {
                    path.moveTo(x1, y1.coerceIn(0f, height))
                }
                path.lineTo(x2, y2.coerceIn(0f, height))
            }
            canvas.drawPath(path, paint)
        }
    }
}