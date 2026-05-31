package com.airmouse.touchpad

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.airmouse.R
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import java.util.UUID

class TouchpadFragment : Fragment() {

    // TCP sender (set by parent)
    var tcpSender: ((String) -> Unit)? = null

    // UI elements
    private lateinit var touchSurface: View
    private lateinit var hintOverlay: View
    private lateinit var clickFeedback: View

    // Haptic feedback
    private var vibrator: Vibrator? = null

    // Touch state
    private var activePointers = mutableMapOf<Int, PointerInfo>()
    private var isScrolling = false
    private var scrollStartY = 0f
    private var scrollVelocity = 0f
    private var scrollMomentumActive = false

    // Click detection
    private var lastTapTime = 0L
    private var doubleTapTimeout = 300L
    private var longPressRunnable: Runnable? = null
    private var longPressTimeout = 500L
    private var isLongPress = false
    private var twoFingerTapStart = 0L
    private val twoFingerTapTimeout = 200L

    // Movement inertia
    private var velocityX = 0f
    private var velocityY = 0f
    private var lastMoveTime = 0L
    private var inertiaActive = false
    private val friction = 0.95f
    private val moveThreshold = 0.5f
    private val scrollFriction = 0.9f

    // Acceleration curve
    private fun accelerate(value: Float): Float {
        val sign = if (value >= 0) 1f else -1f
        val absVal = abs(value)
        return sign * (absVal.pow(1.5f) * 0.3f)
    }

    // Haptic feedback helper
    private fun hapticFeedback(durationMs: Long = 30, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }

    // Visual feedback (ripple effect)
    private fun showClickFeedback(x: Float, y: Float) {
        clickFeedback.apply {
            translationX = x - width / 2
            translationY = y - height / 2
            visibility = View.VISIBLE
            alpha = 0.8f
            animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        visibility = View.GONE
                    }
                })
                .start()
        }
    }

    // Send JSON message via TCP
    private fun send(json: String) {
        tcpSender?.invoke(json)
    }

    private fun genId() = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vibrator = ContextCompat.getSystemService(requireContext(), Vibrator::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_touchpad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        touchSurface = view.findViewById(R.id.touchSurface)
        hintOverlay = view.findViewById(R.id.hintOverlay)
        clickFeedback = view.findViewById(R.id.clickFeedback)

        // Dismiss hint on button click or on first touch
        view.findViewById<View>(R.id.hintDismissBtn).setOnClickListener {
            hintOverlay.visibility = View.GONE
        }

        touchSurface.setOnTouchListener { _, event ->
            if (hintOverlay.visibility == View.VISIBLE) {
                hintOverlay.visibility = View.GONE
            }
            handleTouch(event)
            true
        }
    }

    private fun handleTouch(event: MotionEvent) {
        val actionMasked = event.actionMasked
        when (actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown(event)
            MotionEvent.ACTION_POINTER_DOWN -> handlePointerDown(event)
            MotionEvent.ACTION_MOVE -> handleMove(event)
            MotionEvent.ACTION_UP -> handleUp(event)
            MotionEvent.ACTION_POINTER_UP -> handlePointerUp(event)
            MotionEvent.ACTION_CANCEL -> resetAll()
        }
    }

    private fun handleDown(event: MotionEvent) {
        // Cancel any ongoing inertia
        inertiaActive = false
        scrollMomentumActive = false

        val id = event.getPointerId(0)
        activePointers[id] = PointerInfo(event.x, event.y, System.currentTimeMillis())

        // Reset velocities
        velocityX = 0f
        velocityY = 0f
        lastMoveTime = System.currentTimeMillis()
        isScrolling = false
        isLongPress = false

        // Long press detection
        longPressRunnable = Runnable {
            if (activePointers.size == 1 && !isScrolling) {
                isLongPress = true
                // Long press could trigger right click or custom action
                hapticFeedback(40)
                send("""{"type":"rightclick","id":"${genId()}"}""")
                showClickFeedback(event.x, event.y)
            }
        }
        touchSurface.postDelayed(longPressRunnable, longPressTimeout)
    }

    private fun handlePointerDown(event: MotionEvent) {
        val index = event.actionIndex
        val id = event.getPointerId(index)
        activePointers[id] = PointerInfo(event.getX(index), event.getY(index), System.currentTimeMillis())

        // Cancel long press if more than one finger
        if (activePointers.size > 1) {
            longPressRunnable?.let { touchSurface.removeCallbacks(it) }
        }

        when (activePointers.size) {
            2 -> {
                // Two‑finger tap detection
                twoFingerTapStart = System.currentTimeMillis()
                // Two‑finger scroll start
                isScrolling = true
                val pointers = activePointers.values.toList()
                scrollStartY = (pointers[0].y + pointers[1].y) / 2
                scrollVelocity = 0f
            }
            3 -> {
                // Three‑finger gesture (optional)
                hapticFeedback(20)
                // Example: three‑finger swipe up/down? Could be mapped to back/forward or other actions.
            }
        }
    }

    private fun handleMove(event: MotionEvent) {
        val now = System.currentTimeMillis()
        if (activePointers.size == 2 && isScrolling) {
            // Two‑finger scroll (vertical)
            val pointers = activePointers.values.toList()
            val midY = (pointers[0].y + pointers[1].y) / 2
            val dy = scrollStartY - midY
            if (abs(dy) > 5f) {   // threshold
                val scrollAmount = (dy / 30).toInt().coerceIn(-10, 10)
                if (scrollAmount != 0) {
                    send("""{"type":"scroll","delta":$scrollAmount,"id":"${genId()}"}""")
                    hapticFeedback(15)
                    // For horizontal swipe detection (two‑finger horizontal)
                    val pointersX = activePointers.values.toList()
                    val midX = (pointersX[0].x + pointersX[1].x) / 2
                    // We'll detect horizontal swipe separately if needed
                }
                scrollVelocity = dy / (now - lastMoveTime + 1).toFloat() * 50f
                scrollStartY = midY
            }
            lastMoveTime = now
        } else if (activePointers.size == 1 && !isScrolling) {
            // Single‑finger move
            val pointer = activePointers.values.first()
            val dx = event.x - pointer.x
            val dy = event.y - pointer.y
            if (abs(dx) > moveThreshold || abs(dy) > moveThreshold) {
                val acceleratedX = accelerate(dx)
                val acceleratedY = accelerate(dy)
                send("""{"type":"move","dx":$acceleratedX,"dy":$acceleratedY}""")
                // Update pointer position
                pointer.x = event.x
                pointer.y = event.y
                val dt = (now - lastMoveTime).coerceAtLeast(1)
                velocityX = dx / dt * 50f
                velocityY = dy / dt * 50f
                lastMoveTime = now
            }
        }
    }

    private fun handleUp(event: MotionEvent) {
        longPressRunnable?.let { touchSurface.removeCallbacks(it) }
        val now = System.currentTimeMillis()

        if (activePointers.size == 1 && !isScrolling) {
            // Single‑finger up – tap detection
            if (!isLongPress && now - lastTapTime < doubleTapTimeout) {
                // Double tap
                send("""{"type":"doubleclick","id":"${genId()}"}""")
                hapticFeedback(60)
                showClickFeedback(event.x, event.y)
                lastTapTime = 0
            } else {
                // Single tap (wait for double tap timeout)
                lastTapTime = now
                touchSurface.postDelayed({
                    if (lastTapTime == now && !isLongPress) {
                        send("""{"type":"click","id":"${genId()}"}""")
                        hapticFeedback(30)
                        showClickFeedback(event.x, event.y)
                    }
                }, doubleTapTimeout)
            }
            // Start movement inertia
            startInertia()
        } else if (activePointers.size == 2 && isScrolling) {
            // Two‑finger scroll ended, start scroll momentum
            startScrollMomentum()
        } else if (activePointers.size == 2 && now - twoFingerTapStart < twoFingerTapTimeout && !isScrolling) {
            // Two‑finger tap (right click)
            send("""{"type":"rightclick","id":"${genId()}"}""")
            hapticFeedback(50)
            showClickFeedback(event.x, event.y)
        }

        // Remove the pointer that was lifted
        val id = if (event.actionMasked == MotionEvent.ACTION_UP) {
            event.getPointerId(0)
        } else {
            event.getPointerId(event.actionIndex)
        }
        activePointers.remove(id)

        if (activePointers.isEmpty()) {
            isScrolling = false
            isLongPress = false
        }
    }

    private fun handlePointerUp(event: MotionEvent) {
        val index = event.actionIndex
        val id = event.getPointerId(index)
        activePointers.remove(id)
        if (activePointers.size == 1) {
            // If one finger remains, cancel scrolling
            isScrolling = false
        }
    }

    private fun startInertia() {
        if (abs(velocityX) < 0.1f && abs(velocityY) < 0.1f) return
        inertiaActive = true
        touchSurface.post(object : Runnable {
            override fun run() {
                if (!inertiaActive) return
                velocityX *= friction
                velocityY *= friction
                if (abs(velocityX) < 0.05f && abs(velocityY) < 0.05f) {
                    inertiaActive = false
                    return
                }
                send("""{"type":"move","dx":${velocityX},"dy":${velocityY}}""")
                touchSurface.postDelayed(this, 16)
            }
        })
    }

    private fun startScrollMomentum() {
        if (abs(scrollVelocity) < 0.1f) return
        scrollMomentumActive = true
        touchSurface.post(object : Runnable {
            override fun run() {
                if (!scrollMomentumActive) return
                scrollVelocity *= scrollFriction
                if (abs(scrollVelocity) < 0.1f) {
                    scrollMomentumActive = false
                    return
                }
                val delta = scrollVelocity.toInt().coerceIn(-5, 5)
                if (delta != 0) {
                    send("""{"type":"scroll","delta":$delta,"id":"${genId()}"}""")
                }
                touchSurface.postDelayed(this, 16)
            }
        })
    }

    private fun resetAll() {
        inertiaActive = false
        scrollMomentumActive = false
        isScrolling = false
        isLongPress = false
        activePointers.clear()
        longPressRunnable?.let { touchSurface.removeCallbacks(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        resetAll()
    }

    private data class PointerInfo(var x: Float, var y: Float, var startTime: Long)
}