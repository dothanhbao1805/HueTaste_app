package com.example.huetaste.common.ui.gallery

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Scale
import coil.target.Target
import com.base.common.widget.LsImageView
import kotlin.math.abs

/**
 * Adds pinch zoom, panning and double-tap zoom to an [LsImageView].
 *
 * `LsImageView` is final, so the behavior is attached by composition instead of a subclass. The image is
 * fitted to the view, then a user matrix scales it between 1x and [MAX_SCALE]. While zoomed, touches are
 * kept from the parent pager until the image is panned to an edge, so swiping there changes the page.
 */
class ImageZoomController(
    private val imageView: LsImageView,
    private val onSingleTap: () -> Unit,
) {
    private val baseMatrix = Matrix()
    private val userMatrix = Matrix()
    private val drawMatrix = Matrix()
    private val values = FloatArray(9)
    private val displayRect = RectF()
    private var zoomAnimator: ValueAnimator? = null
    private var request: Disposable? = null

    private val scaleDetector = ScaleGestureDetector(
        imageView.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val current = currentScale()
                val target = (current * detector.scaleFactor).coerceIn(MIN_SCALE, MAX_SCALE)
                zoomBy(target / current, detector.focusX, detector.focusY)
                return true
            }
        },
    )

    private val gestureDetector = GestureDetector(
        imageView.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (scaleDetector.isInProgress || currentScale() <= MIN_SCALE + SCALE_EPSILON) return false
                val rect = displayRect()
                val atEdge = (distanceX < 0 && rect.left >= -EDGE_EPSILON) ||
                    (distanceX > 0 && rect.right <= imageView.width + EDGE_EPSILON)
                // At a horizontal edge the pager takes over; otherwise the image keeps the gesture.
                imageView.parent?.requestDisallowInterceptTouchEvent(!atEdge)
                if (atEdge && abs(distanceX) > abs(distanceY)) return false
                userMatrix.postTranslate(-distanceX, -distanceY)
                applyMatrix()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val target = if (currentScale() > MIN_SCALE + SCALE_EPSILON) MIN_SCALE else DOUBLE_TAP_SCALE
                animateZoomTo(target, e.x, e.y)
                return true
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onSingleTap()
                return true
            }
        },
    )

    init {
        imageView.scaleType = ImageView.ScaleType.MATRIX
        imageView.setOnTouchListener(::onTouch)
        imageView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) resetZoom()
        }
        imageView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                zoomAnimator?.cancel()
            }
        })
    }

    /**
     * Loads [url] at up to [MAX_IMAGE_PX] so zoomed detail stays sharp. [placeholderKey] is the memory cache key
     * of a smaller bitmap (the hero photo) to show while the full image loads.
     */
    fun load(url: String, placeholderKey: String?) {
        request?.dispose()
        imageView.setImageDrawable(null)
        resetZoom()
        request = imageView.context.imageLoader.enqueue(
            ImageRequest.Builder(imageView.context)
                .data(url)
                .placeholderMemoryCacheKey(placeholderKey)
                .size(MAX_IMAGE_PX, MAX_IMAGE_PX)
                .scale(Scale.FIT)
                .crossfade(true)
                .target(object : Target {
                    override fun onStart(placeholder: Drawable?) = show(placeholder)
                    override fun onSuccess(result: Drawable) = show(result)
                })
                .build(),
        )
    }

    private fun show(drawable: Drawable?) {
        imageView.setImageDrawable(drawable)
        resetZoom()
    }

    private fun resetZoom() {
        zoomAnimator?.cancel()
        userMatrix.reset()
        val drawable = imageView.drawable
        if (drawable == null || imageView.width == 0 || imageView.height == 0) {
            baseMatrix.reset()
        } else {
            val source = RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            val target = RectF(0f, 0f, imageView.width.toFloat(), imageView.height.toFloat())
            baseMatrix.setRectToRect(source, target, Matrix.ScaleToFit.CENTER)
        }
        applyMatrix()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun onTouch(view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                zoomAnimator?.cancel()
                view.parent?.requestDisallowInterceptTouchEvent(currentScale() > MIN_SCALE + SCALE_EPSILON)
            }
            MotionEvent.ACTION_POINTER_DOWN -> view.parent?.requestDisallowInterceptTouchEvent(true)
        }
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun animateZoomTo(target: Float, focusX: Float, focusY: Float) {
        zoomAnimator?.cancel()
        zoomAnimator = ValueAnimator.ofFloat(currentScale(), target).apply {
            duration = ZOOM_ANIMATION_MS
            addUpdateListener { animator ->
                zoomBy((animator.animatedValue as Float) / currentScale(), focusX, focusY)
            }
            start()
        }
    }

    private fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
        userMatrix.postScale(factor, factor, focusX, focusY)
        applyMatrix()
    }

    private fun currentScale(): Float {
        userMatrix.getValues(values)
        return values[Matrix.MSCALE_X]
    }

    /** The image rectangle in view coordinates, after both matrices. */
    private fun displayRect(): RectF {
        val drawable = imageView.drawable ?: return displayRect.apply { setEmpty() }
        displayRect.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(userMatrix)
        drawMatrix.mapRect(displayRect)
        return displayRect
    }

    private fun applyMatrix() {
        if (imageView.drawable == null) return
        keepInsideView()
        drawMatrix.set(baseMatrix)
        drawMatrix.postConcat(userMatrix)
        imageView.imageMatrix = drawMatrix
    }

    /** Centers an axis that is smaller than the view and stops the image from leaving the view on the other. */
    private fun keepInsideView() {
        val rect = displayRect()
        val viewWidth = imageView.width.toFloat()
        val viewHeight = imageView.height.toFloat()
        val dx = when {
            rect.width() <= viewWidth -> (viewWidth - rect.width()) / 2f - rect.left
            rect.left > 0f -> -rect.left
            rect.right < viewWidth -> viewWidth - rect.right
            else -> 0f
        }
        val dy = when {
            rect.height() <= viewHeight -> (viewHeight - rect.height()) / 2f - rect.top
            rect.top > 0f -> -rect.top
            rect.bottom < viewHeight -> viewHeight - rect.bottom
            else -> 0f
        }
        userMatrix.postTranslate(dx, dy)
    }

    private companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 4f
        const val DOUBLE_TAP_SCALE = 2.5f
        const val SCALE_EPSILON = 0.01f
        const val EDGE_EPSILON = 1f
        const val ZOOM_ANIMATION_MS = 200L
        const val MAX_IMAGE_PX = 2048
    }
}
