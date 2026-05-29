package com.airmouse.touchpad

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow

class TouchpadFragment : Fragment() {

    var tcpSender: ((String) -> Unit)? = null

    // ---------- Touch state ----------
    private var lastX = 0f
    private var lastY = 0f
    private var isScrolling = false
    private var scrollStartY = 0f
    private var lastTapTime = 0L
    private var doubleTapTimeout = 300L
    private var longPressTimeout = 500L
    private var longPressRunnable: Runnable? = null
    private var isLongPress = false
    private var isRightClickCandidate = false   // two-finger tap detection
    private var twoFingerStartTime = 0L

    // ---------- Acceleration ----------
    private var velocityX = 0f
    private var velocityY = 0f
    private var lastMoveTime = 0L
    private var inertiaActive = false
    private val friction = 0.95f
    private val moveThreshold = 0.5f         // minimum pixel change to send move
    private val scrollThreshold = 10f        // minimum scroll delta to send

    // ---------- Scroll momentum ----------
    private var scrollVelocity = 0f
    private var scrollMomentumActive = false
    private val scrollFriction = 0.9f

    // ---------- Haptic feedback ----------
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vibrator = context?.getSystemService()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_touchpad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.touchSurface)?.setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
    }

    // ---------------------------------------------------------------
    //  Main touch dispatcher
    // ---------------------------------------------------------------
    private fun handleTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event)
            MotionEvent.ACTION_MOVE -> handleMove(event)
            MotionEvent.ACTION_UP -> handleUp(event)
            MotionEvent.ACTION_POINTER_UP -> handlePointerUp(event)
            MotionEvent.ACTION_CANCEL -> resetAll()
        }
    }

    // ---------------------------------------------------------------
    //  Gesture handlers
    // ---------------------------------------------------------------
    private fun handleDown(event: MotionEvent) {
        // Cancel any ongoing inertia
        inertiaActive = false
        scrollMomentumActive = false

        lastX = event.x
        lastY = event.y
        lastMoveTime = System.currentTimeMillis()
        velocityX = 0f
        velocityY = 0f
        isScrolling = false
        isRightClickCandidate = false
        isLongPress = false

        // Start long‑press detection
        longPressRunnable = Runnable {
            if (!isScrolling && event.pointerCount == 1) {
                isLongPress = true
                // Long press → nothing by default, but can be used later
            }
        }
        view?.postDelayed(longPressRunnable, longPressTimeout)
    }

    private fun handlePointerDown(event: MotionEvent) {
        longPressRunnable?.let { view?.removeCallbacks(it) }
        when (event.pointerCount) {
            2 -> {
                // Potential two‑finger scroll or right‑click
                isScrolling = true
                scrollStartY = (event.getY(0) + event.getY(1)) / 2
                twoFingerStartTime = System.currentTimeMillis()
                scrollVelocity = 0f
            }
            3 -> {
                // Three‑finger gesture – we ignore for now (could map to swipe navigation)
            }
        }
    }

    private fun handleMove(event: MotionEvent) {
        val now = System.currentTimeMillis()
        if (isScrolling && event.pointerCount == 2) {
            // Two‑finger scroll (vertical)
            val midY = (event.getY(0) + event.getY(1)) / 2
            val dy = scrollStartY - midY
            if (abs(dy) > scrollThreshold) {
                // Scale delta for smoothness
                val scrollAmount = (dy / 30).toInt().coerceIn(-5, 5)   // discrete steps, but big steps for speed
                if (scrollAmount != 0) {
                    send("""{"type":"scroll","delta":$scrollAmount,"id":"${genId()}"}""")
                }
                // Track velocity for momentum
                scrollVelocity = dy / (now - lastMoveTime + 1).toFloat() * 50f
                scrollStartY = midY
            }
            lastMoveTime = now
        } else if (!isScrolling && event.pointerCount == 1) {
            // Single‑finger move
            val dx = event.x - lastX
            val dy = event.y - lastY
            if (abs(dx) > moveThreshold || abs(dy) > moveThreshold) {
                // Apply acceleration curve (Expo ease)
                val acceleratedX = accelerate(dx)
                val acceleratedY = accelerate(dy)
                send("""{"type":"move","dx":$acceleratedX,"dy":$acceleratedY}""")
                lastX = event.x
                lastY = event.y
                // Update velocity for inertia
                val dt = (now - lastMoveTime).coerceAtLeast(1)
                velocityX = dx / dt * 50f
                velocityY = dy / dt * 50f
                lastMoveTime = now
            }
        }
    }

    private fun handleUp(event: MotionEvent) {
        longPressRunnable?.let { view?.removeCallbacks(it) }
        if (isScrolling) {
            // Start scroll momentum if needed
            startScrollMomentum()
            isScrolling = false
            return
        }
        if (isLongPress) {
            isLongPress = false
            return
        }
        if (event.pointerCount == 1 && !isScrolling) {
            // Detect single or double tap
            val now = System.currentTimeMillis()
            if (now - lastTapTime < doubleTapTimeout) {
                send("""{"type":"doubleclick","id":"${genId()}"}""")
                lastTapTime = 0
                hapticFeedback(60)
            } else {
                lastTapTime = now
                view?.postDelayed({
                    if (lastTapTime == now) {
                        send("""{"type":"click","id":"${genId()}"}""")
                        hapticFeedback(30)
                    }
                }, doubleTapTimeout)
            }
            // Start movement inertia
            startInertia()
        }
    }

    private fun handlePointerUp(event: MotionEvent) {
        if (event.pointerCount == 2) {
            // Two fingers were lifted – check for two‑finger tap (right click)
            val now = System.currentTimeMillis()
            if (now - twoFingerStartTime < 200 && !isScrolling) {
                send("""{"type":"rightclick","id":"${genId()}"}""")
                hapticFeedback(50)
            }
            isScrolling = false
            startScrollMomentum()
        }
    }

    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------
    private fun accelerate(value: Float): Float {
        val sign = if (value >= 0) 1f else -1f
        val absVal = abs(value)
        // Exponential curve: feels like Apple trackpad
        return sign * (absVal.pow(1.5f) * 0.3f)
    }

    private fun startInertia() {
        if (abs(velocityX) < 0.1f && abs(velocityY) < 0.1f) return
        inertiaActive = true
        view?.post(object : Runnable {
            override fun run() {
                if (!inertiaActive) return
                velocityX *= friction
                velocityY *= friction
                if (abs(velocityX) < 0.05f && abs(velocityY) < 0.05f) {
                    inertiaActive = false
                    return
                }
                send("""{"type":"move","dx":${velocityX},"dy":${velocityY}}""")
                view?.postDelayed(this, 16)
            }
        })
    }

    private fun startScrollMomentum() {
        if (abs(scrollVelocity) < 0.1f) return
        scrollMomentumActive = true
        view?.post(object : Runnable {
            override fun run() {
                if (!scrollMomentumActive) return
                scrollVelocity *= scrollFriction
                if (abs(scrollVelocity) < 0.1f) {
                    scrollMomentumActive = false
                    return
                }
                val delta = scrollVelocity.toInt().coerceIn(-3, 3)
                if (delta != 0) {
                    send("""{"type":"scroll","delta":$delta,"id":"${genId()}"}""")
                }
                view?.postDelayed(this, 16)
            }
        })
    }

    private fun resetAll() {
        inertiaActive = false
        scrollMomentumActive = false
        isScrolling = false
        isLongPress = false
        longPressRunnable?.let { view?.removeCallbacks(it) }
    }

    private fun hapticFeedback(durationMs: Long) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }

    private fun send(json: String) {
        tcpSender?.invoke(json)
    }

    private fun genId() = UUID.randomUUID().toString()

    override fun onDestroyView() {
        super.onDestroyView()
        resetAll()
    }
}