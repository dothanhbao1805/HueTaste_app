package com.example.huetaste.common.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.example.huetaste.R

/**
 * A light band sweeping across the image area while a photo loads. It has no intrinsic size, so it fills its view
 * whatever the scale type. It redraws itself only while it is being drawn, so it stops by itself once replaced or
 * off screen. The sweep follows the clock, which keeps every image on a screen in step.
 */
class ImageShimmerDrawable(context: Context) : Drawable() {
    private val baseColor = ContextCompat.getColor(context, R.color.hue_surface_container_high)
    private val highlightColor = ContextCompat.getColor(context, R.color.hue_surface_container_low)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sweep = Matrix()

    override fun onBoundsChange(bounds: Rect) {
        paint.shader = if (bounds.isEmpty) {
            null
        } else {
            LinearGradient(
                0f,
                0f,
                bounds.width().toFloat(),
                0f,
                intArrayOf(baseColor, highlightColor, baseColor),
                null,
                Shader.TileMode.CLAMP,
            )
        }
    }

    override fun draw(canvas: Canvas) {
        val shader = paint.shader ?: return
        val width = bounds.width().toFloat()
        val progress = (SystemClock.uptimeMillis() % SWEEP_MS) / SWEEP_MS.toFloat()
        // The band enters fully off the left edge and leaves fully off the right one.
        sweep.setTranslate(bounds.left - width + 2 * width * progress, 0f)
        shader.setLocalMatrix(sweep)
        canvas.drawRect(bounds, paint)
        invalidateSelf()
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val SWEEP_MS = 1200L
    }
}
