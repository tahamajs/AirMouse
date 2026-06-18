package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import com.airmouse.network.ConnectionManager
import com.airmouse.R
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class EdgeGestureService : Service() {

    @Inject lateinit var connectionManager: ConnectionManager
    @Inject lateinit var prefs: PreferencesManager

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var isActive = false
    private var isExpanded = false
    private var startX = 0f
    private var startY = 0f
    private var initialX = 0
    private var initialY = 0
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var hideJob: Job? = null

    companion object {
        private const val EDGE_SIZE = 80
        private const val COLLAPSED_SIZE = 48
        private const val EXPANDED_SIZE = 120

        fun startService(context: android.content.Context) {
            val intent = Intent(context, EdgeGestureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: android.content.Context) {
            context.stopService(Intent(context, EdgeGestureService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (prefs.getBoolean("edge_gestures_enabled", true)) {
            startEdgeGestures()
        }
    }

    fun startEdgeGestures() {
        if (isActive) return
        isActive = true

        createFloatingView()
    }

    fun stopEdgeGestures() {
        if (!isActive) return
        isActive = false

        floatingView?.let { windowManager.removeView(it) }
        floatingView = null
        hideJob?.cancel()
    }

    private fun createFloatingView() {
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.view_edge_gesture, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        windowManager.addView(floatingView, params)

        setupTouchListener()
        setupActionButtons()

        scheduleHide()
    }

    private fun setupTouchListener() {
        floatingView?.setOnTouchListener { view, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    initialX = (view.layoutParams as WindowManager.LayoutParams).x
                    initialY = (view.layoutParams as WindowManager.LayoutParams).y
                    expandView()
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    val dy = event.rawY - startY
                    val params = view.layoutParams as WindowManager.LayoutParams
                    params.x = initialX + dx.toInt()
                    params.y = initialY + dy.toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    scheduleHide()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupActionButtons() {
        val btnClick = floatingView?.findViewById<ImageView>(R.id.btn_click)
        val btnRightClick = floatingView?.findViewById<ImageView>(R.id.btn_right_click)
        val btnScrollUp = floatingView?.findViewById<ImageView>(R.id.btn_scroll_up)
        val btnScrollDown = floatingView?.findViewById<ImageView>(R.id.btn_scroll_down)
        val btnClose = floatingView?.findViewById<ImageView>(R.id.btn_close)

        btnClick?.setOnClickListener {
            connectionManager.sendClick()
            hideView()
        }

        btnRightClick?.setOnClickListener {
            connectionManager.sendRightClick()
            hideView()
        }

        btnScrollUp?.setOnClickListener {
            connectionManager.sendScroll(3)
            hideView()
        }

        btnScrollDown?.setOnClickListener {
            connectionManager.sendScroll(-3)
            hideView()
        }

        btnClose?.setOnClickListener {
            stopEdgeGestures()
        }
    }

    private fun expandView() {
        if (isExpanded) return
        isExpanded = true

        floatingView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.width = dpToPx(EXPANDED_SIZE)
            params.height = dpToPx(EXPANDED_SIZE)
            windowManager.updateViewLayout(view, params)

            val container = view.findViewById<FrameLayout>(R.id.edge_gesture_container)
            container?.alpha = 1f
        }
    }

    private fun collapseView() {
        if (!isExpanded) return
        isExpanded = false

        floatingView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            params.width = dpToPx(COLLAPSED_SIZE)
            params.height = dpToPx(COLLAPSED_SIZE)
            windowManager.updateViewLayout(view, params)

            val container = view.findViewById<FrameLayout>(R.id.edge_gesture_container)
            container?.alpha = 0f
        }
    }

    private fun hideView() {
        floatingView?.alpha = 0f
        hideJob = serviceScope.launch {
            delay(300)
            if (!isExpanded) {
                floatingView?.alpha = 1f
            }
        }
    }

    private fun scheduleHide() {
        hideJob?.cancel()
        hideJob = serviceScope.launch {
            delay(3000)
            if (!isExpanded) {
                collapseView()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopEdgeGestures()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}