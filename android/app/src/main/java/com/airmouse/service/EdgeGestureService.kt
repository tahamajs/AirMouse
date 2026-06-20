//// app/src/main/java/com/airmouse/service/EdgeGestureService.kt
//package com.airmouse.service
//
//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import android.view.Gravity
//import android.view.LayoutInflater
//import android.view.View
//import android.view.WindowManager
//import android.widget.FrameLayout
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.core.app.NotificationCompat
//import com.airmouse.R
//import com.airmouse.network.ConnectionManager
//import com.airmouse.utils.PreferencesManager
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class EdgeGestureService : Service() {
//
//    @Inject lateinit var connectionManager: ConnectionManager
//    @Inject lateinit var prefs: PreferencesManager
//
//    private lateinit var windowManager: WindowManager
//    private var floatingView: View? = null
//    private var isActive = false
//    private var isExpanded = false
//    private var startX = 0f
//    private var startY = 0f
//    private var initialX = 0
//    private var initialY = 0
//    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
//    private var hideJob: Job? = null
//    private var connectionCheckJob: Job? = null
//
//    companion object {
//        private const val NOTIFICATION_ID = 2002
//        private const val CHANNEL_ID = "edge_gesture_channel"
//
//        fun startService(context: Context) {
//            val intent = Intent(context, EdgeGestureService::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(intent)
//            } else {
//                context.startService(intent)
//            }
//        }
//
//        fun stopService(context: Context) {
//            context.stopService(Intent(context, EdgeGestureService::class.java))
//        }
//    }
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        startForeground(NOTIFICATION_ID, createNotification("Edge Gestures", "Active"))
//        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
//
//        if (prefs.getBoolean("edge_gestures_enabled", true)) {
//            startEdgeGestures()
//        }
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "Edge Gestures",
//                NotificationManager.IMPORTANCE_LOW
//            ).apply {
//                description = "Edge gesture controls"
//            }
//            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
//        }
//    }
//
//    private fun createNotification(title: String, content: String): Notification {
//        return NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle(title)
//            .setContentText(content)
//            .setSmallIcon(R.drawable.ic_gesture)
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .build()
//    }
//
//    fun startEdgeGestures() {
//        if (isActive) return
//        isActive = true
//        createFloatingView()
//        startConnectionCheck()
//        updateNotification("Edge Gestures", "Active")
//    }
//
//    fun stopEdgeGestures() {
//        if (!isActive) return
//        isActive = false
//        connectionCheckJob?.cancel()
//
//        floatingView?.let {
//            try {
//                windowManager.removeView(it)
//            } catch (e: Exception) {
//                // View already removed
//            }
//        }
//        floatingView = null
//        hideJob?.cancel()
//        updateNotification("Edge Gestures", "Stopped")
//    }
//
//    private fun createFloatingView() {
//        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
//        floatingView = inflater.inflate(R.layout.view_edge_gesture, null)
//
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            WindowManager.LayoutParams.WRAP_CONTENT,
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
//            else
//                WindowManager.LayoutParams.TYPE_PHONE,
//            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
//            android.graphics.PixelFormat.TRANSLUCENT
//        )
//
//        params.gravity = Gravity.TOP or Gravity.START
//        params.x = 0
//        params.y = 200
//
//        windowManager.addView(floatingView, params)
//
//        setupTouchListener()
//        setupActionButtons()
//        updateConnectionStatus(false)
//        scheduleHide()
//    }
//
//    private fun setupTouchListener() {
//        floatingView?.setOnTouchListener { view, event ->
//            when (event.action) {
//                android.view.MotionEvent.ACTION_DOWN -> {
//                    startX = event.rawX
//                    startY = event.rawY
//                    val params = view.layoutParams as WindowManager.LayoutParams
//                    initialX = params.x
//                    initialY = params.y
//                    expandView()
//                    true
//                }
//                android.view.MotionEvent.ACTION_MOVE -> {
//                    val dx = event.rawX - startX
//                    val dy = event.rawY - startY
//                    val params = view.layoutParams as WindowManager.LayoutParams
//                    params.x = initialX + dx.toInt()
//                    params.y = initialY + dy.toInt()
//                    windowManager.updateViewLayout(view, params)
//                    true
//                }
//                android.view.MotionEvent.ACTION_UP -> {
//                    scheduleHide()
//                    true
//                }
//                else -> false
//            }
//        }
//    }
//
//    private fun setupActionButtons() {
//        val btnClick = floatingView?.findViewById<ImageView>(R.id.btn_click)
//        val btnRightClick = floatingView?.findViewById<ImageView>(R.id.btn_right_click)
//        val btnScrollUp = floatingView?.findViewById<ImageView>(R.id.btn_scroll_up)
//        val btnScrollDown = floatingView?.findViewById<ImageView>(R.id.btn_scroll_down)
//        val btnClose = floatingView?.findViewById<ImageView>(R.id.btn_close)
//        val btnStatus = floatingView?.findViewById<TextView>(R.id.btn_status)
//
//        btnClick?.setOnClickListener {
//            if (connectionManager.isConnected()) {
//                connectionManager.sendClick()
//                showFeedback("Click")
//                hideView()
//            } else {
//                showFeedback("Not Connected")
//            }
//        }
//
//        btnRightClick?.setOnClickListener {
//            if (connectionManager.isConnected()) {
//                connectionManager.sendRightClick()
//                showFeedback("Right Click")
//                hideView()
//            } else {
//                showFeedback("Not Connected")
//            }
//        }
//
//        btnScrollUp?.setOnClickListener {
//            if (connectionManager.isConnected()) {
//                connectionManager.sendScroll(3)
//                showFeedback("Scroll Up")
//                hideView()
//            } else {
//                showFeedback("Not Connected")
//            }
//        }
//
//        btnScrollDown?.setOnClickListener {
//            if (connectionManager.isConnected()) {
//                connectionManager.sendScroll(-3)
//                showFeedback("Scroll Down")
//                hideView()
//            } else {
//                showFeedback("Not Connected")
//            }
//        }
//
//        btnClose?.setOnClickListener {
//            stopEdgeGestures()
//        }
//
//        btnStatus?.let { statusView ->
//            // Update status on the floating view
//            updateConnectionStatus(connectionManager.isConnected())
//        }
//    }
//
//    private fun expandView() {
//        if (isExpanded) return
//        isExpanded = true
//
//        floatingView?.let { view ->
//            val params = view.layoutParams as WindowManager.LayoutParams
//            params.width = dpToPx(120)
//            params.height = dpToPx(120)
//            windowManager.updateViewLayout(view, params)
//
//            val container = view.findViewById<FrameLayout>(R.id.edge_gesture_container)
//            container?.alpha = 1f
//
//            // Update status
//            updateConnectionStatus(connectionManager.isConnected())
//        }
//    }
//
//    private fun collapseView() {
//        if (!isExpanded) return
//        isExpanded = false
//
//        floatingView?.let { view ->
//            val params = view.layoutParams as WindowManager.LayoutParams
//            params.width = dpToPx(48)
//            params.height = dpToPx(48)
//            windowManager.updateViewLayout(view, params)
//
//            val container = view.findViewById<FrameLayout>(R.id.edge_gesture_container)
//            container?.alpha = 0f
//        }
//    }
//
//    private fun hideView() {
//        floatingView?.alpha = 0f
//        hideJob = serviceScope.launch {
//            delay(300)
//            if (!isExpanded) {
//                floatingView?.alpha = 1f
//            }
//        }
//    }
//
//    private fun scheduleHide() {
//        hideJob?.cancel()
//        hideJob = serviceScope.launch {
//            delay(3000)
//            if (!isExpanded) {
//                collapseView()
//            }
//        }
//    }
//
//    private fun showFeedback(message: String) {
//        val statusView = floatingView?.findViewById<TextView>(R.id.btn_status)
//        statusView?.text = message
//        statusView?.postDelayed({
//            updateConnectionStatus(connectionManager.isConnected())
//        }, 1000)
//    }
//
//    private fun updateConnectionStatus(isConnected: Boolean) {
//        val statusView = floatingView?.findViewById<TextView>(R.id.btn_status)
//        statusView?.text = if (isConnected) "🟢 Connected" else "🔴 Disconnected"
//        statusView?.setTextColor(if (isConnected) android.graphics.Color.GREEN else android.graphics.Color.RED)
//    }
//
//    private fun startConnectionCheck() {
//        connectionCheckJob?.cancel()
//        connectionCheckJob = serviceScope.launch {
//            while (isActive) {
//                delay(2000)
//                updateConnectionStatus(connectionManager.isConnected())
//            }
//        }
//    }
//
//    private fun dpToPx(dp: Int): Int {
//        return (dp * resources.displayMetrics.density).toInt()
//    }
//
//    private fun updateNotification(title: String, content: String) {
//        val notification = createNotification(title, content)
//        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        manager.notify(NOTIFICATION_ID, notification)
//        startForeground(NOTIFICATION_ID, notification)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        stopEdgeGestures()
//        serviceScope.cancel()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//}