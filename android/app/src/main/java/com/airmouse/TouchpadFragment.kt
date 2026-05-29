package com.airmouse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlin.math.abs

class TouchpadFragment : Fragment() {

    private var lastX = 0f
    private var lastY = 0f
    private var isScrolling = false
    private var scrollStartY = 0f
    private val scrollThreshold = 20f   // minimum vertical move for scroll
    private var lastTapTime = 0L
    private val doubleTapTimeout = 300L

    // Reference to the TCP sender – you must set this from HomeFragment
    var tcpSender: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_touchpad, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val touchSurface = view.findViewById<View>(R.id.touchSurface)
        touchSurface.setOnTouchListener { _, event ->
            handleTouch(event)
            true
        }
    }

    private fun handleTouch(event: MotionEvent) {
        val action = event.actionMasked
        val pointerCount = event.pointerCount

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (pointerCount == 1) {
                    lastX = event.x
                    lastY = event.y
                    isScrolling = false
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // Two fingers → scroll mode
                if (pointerCount == 2) {
                    isScrolling = true
                    scrollStartY = (event.getY(0) + event.getY(1)) / 2
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScrolling && pointerCount == 2) {
                    val currentMidY = (event.getY(0) + event.getY(1)) / 2
                    val deltaY = scrollStartY - currentMidY
                    if (abs(deltaY) > scrollThreshold) {
                        val scrollAmount = if (deltaY > 0) 1 else -1
                        sendCommand("""{"type":"scroll","delta":$scrollAmount,"id":"${generateId()}"}""")
                        scrollStartY = currentMidY
                    }
                } else if (!isScrolling && pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (abs(dx) > 1 || abs(dy) > 1) {
                        sendCommand("""{"type":"move","dx":$dx,"dy":$dy}""")
                    }
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isScrolling && pointerCount == 1) {
                    // Check for tap
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < doubleTapTimeout) {
                        sendCommand("""{"type":"doubleclick","id":"${generateId()}"}""")
                        lastTapTime = 0
                    } else {
                        lastTapTime = now
                        // Single tap will be sent after a short delay to allow double-tap
                        view?.postDelayed({
                            if (lastTapTime == now) {
                                sendCommand("""{"type":"click","id":"${generateId()}"}""")
                            }
                        }, doubleTapTimeout)
                    }
                }
                isScrolling = false
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (pointerCount == 2) isScrolling = false
            }
        }
    }

    private fun sendCommand(json: String) {
        tcpSender?.invoke(json)
    }

    private fun generateId() = java.util.UUID.randomUUID().toString()
}