package com.base.common.widget

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewPropertyAnimator
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import kotlin.math.abs

class LsGestureHelper(private val targetView: View) {

    companion object {
        private const val MIN_SWIPE_DISTANCE = 50
        private const val ANIMATION_DURATION = 200L
        private const val ANIMATION_DURATION_SHORT = 100L
    }

    enum class Direction {
        Left, Right, Top, Bottom
    }

    var enableGesture = false
        set(value) {
            if (value) targetView.isClickable = true
            field = value
        }
    var flexibility = 2f
        set(value) {
            require(value in 1f..10f) { "Flexibility must be between [1f..10f]" }
            field = value
        }

    var clicks: (pair: Pair<Float, Float>) -> Unit = { }
    var longClicks: (pair: Pair<Float, Float>) -> Unit = { }
    var swipe: (direction: Direction) -> Unit = { }

    private var downX = 0f
    private var downY = 0f
    private var upX = 0f
    private var upY = 0f
    private var isSwipeDetected = false
    private val repeatInterval = 100L // Thời gian giữa các lần lặp
    private var isRepeating = false
    private val repeatHandler = Handler(Looper.getMainLooper())
    private var longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val longPressHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        isRepeating = true
        repeatHandler.post(repeatRunnable)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun attachTo() {
        targetView.setOnTouchListener { _, event ->
            if (!enableGesture) return@setOnTouchListener false
            handleTouchEvent(event)
            false
        }
    }

    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (isRepeating) {
                longClicks(downX to downY)
                repeatHandler.postDelayed(this, repeatInterval)
            }
        }
    }

    private fun handleTouchEvent(event: MotionEvent) {
        Timber.e("Event: $event")
        val rotationY = calculateRotation((event.x * flexibility * 2) / targetView.width)
        val rotationX = -calculateRotation((event.y * flexibility * 2) / targetView.height)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isSwipeDetected = false

                longPressHandler.postDelayed(longPressRunnable, longPressTimeout)

                animate {
                    rotationY(rotationY)
                    rotationX(rotationX)
                    duration = ANIMATION_DURATION_SHORT
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.x - downX
                val deltaY = event.y - downY

                if (!isSwipeDetected && (abs(deltaX) > MIN_SWIPE_DISTANCE || abs(deltaY) > MIN_SWIPE_DISTANCE)) {
                    isSwipeDetected = true
                    longPressHandler.removeCallbacks(longPressRunnable) // huỷ long press nếu swipe
                }

                targetView.rotationY = rotationY
                targetView.rotationX = rotationX
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                upX = event.x
                upY = event.y

                longPressHandler.removeCallbacks(longPressRunnable)
                stopRepeat()

                if (!isSwipeDetected) {
                    clicks(event.rawX to event.rawY)
                } else {
                    detectSwipeDirection()
                }

                resetRotation()
            }
        }
    }

    private fun stopRepeat() {
        isRepeating = false
        repeatHandler.removeCallbacks(repeatRunnable)
    }

    private fun detectSwipeDirection() {
        val deltaX = downX - upX
        val deltaY = downY - upY

        val direction = when {
            abs(deltaX) > abs(deltaY) && abs(deltaX) > MIN_SWIPE_DISTANCE ->
                if (deltaX > 0) Direction.Left else Direction.Right

            abs(deltaY) > MIN_SWIPE_DISTANCE ->
                if (deltaY > 0) Direction.Top else Direction.Bottom

            else -> null
        }

        direction?.let { swipe(direction) }
    }

    private fun resetRotation() {
        targetView.clearAnimation()
        animate {
            rotationX(0f)
            rotationY(0f)
            duration = ANIMATION_DURATION
        }
    }

    private fun calculateRotation(value: Float): Float {
        val clamped = value.coerceIn(0f, flexibility * 2)
        return clamped - flexibility
    }

    private fun animate(body: ViewPropertyAnimator.() -> Unit) {
        targetView.animate().apply {
            interpolator = FastOutSlowInInterpolator()
            body()
            start()
        }
    }
}