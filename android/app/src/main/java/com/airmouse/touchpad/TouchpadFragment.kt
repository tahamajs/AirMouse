package com.airmouse.touchpad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import java.util.UUID
import kotlin.math.abs

class TouchpadFragment : Fragment() {

    var tcpSender: ((String) -> Unit)? = null

    private var lastX = 0f
    private var lastY = 0f
    private var isScrolling = false
    private var scrollStartY = 0f
    private var lastTapTime = 0L
    private val doubleTapTimeout = 300L

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

    private fun handleTouch(event: MotionEvent) {
        val pointerCount = event.pointerCount
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (pointerCount == 1) {
                    lastX = event.x
                    lastY = event.y
                    isScrolling = false
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    isScrolling = true
                    scrollStartY = (event.getY(0) + event.getY(1)) / 2
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isScrolling && pointerCount == 2) {
                    val midY = (event.getY(0) + event.getY(1)) / 2
                    val dy = scrollStartY - midY
                    if (abs(dy) > 20f) {
                        val delta = if (dy > 0) 1 else -1
                        send("""{"type":"scroll","delta":$delta,"id":"${genId()}"}""")
                        scrollStartY = midY
                    }
                } else if (!isScrolling && pointerCount == 1) {
                    val dx = event.x - lastX
                    val dy = event.y - lastY
                    if (abs(dx) > 1 || abs(dy) > 1) {
                        send("""{"type":"move","dx":$dx,"dy":$dy}""")
                    }
                    lastX = event.x
                    lastY = event.y
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!isScrolling && pointerCount == 1) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < doubleTapTimeout) {
                        send("""{"type":"doubleclick","id":"${genId()}"}""")
                        lastTapTime = 0
                    } else {
                        lastTapTime = now
                        view?.postDelayed({
                            if (lastTapTime == now) {
                                send("""{"type":"click","id":"${genId()}"}""")
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

    private fun send(json: String) {
        tcpSender?.invoke(json)
    }

    private fun genId() = UUID.randomUUID().toString()
}