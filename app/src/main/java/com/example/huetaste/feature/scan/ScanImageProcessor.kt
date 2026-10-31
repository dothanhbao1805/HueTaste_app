package com.example.huetaste.feature.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/** Turns camera frames and gallery picks into small JPEG files the scan API accepts (at most 5 MB). */
class ScanImageProcessor @Inject constructor(@ApplicationContext private val context: Context) {

    private val outputDir get() = File(context.cacheDir, "scan").apply { mkdirs() }

    /**
     * Rotates [frame] upright and keeps only what the user saw inside the scan frame.
     *
     * [viewWidth]/[viewHeight] are the PreviewView size and [frameRect] the scan frame in the same
     * coordinates; the preview uses FILL_CENTER, so the frame is mapped back through that scaling.
     */
    suspend fun cropCapture(
        frame: Bitmap,
        rotationDegrees: Int,
        viewWidth: Int,
        viewHeight: Int,
        frameRect: Rect,
    ): File = withContext(Dispatchers.Default) {
        val upright = frame.rotate(rotationDegrees)
        val cropped = if (viewWidth > 0 && viewHeight > 0) {
            val scale = max(viewWidth.toFloat() / upright.width, viewHeight.toFloat() / upright.height)
            val offsetX = (upright.width * scale - viewWidth) / 2f
            val offsetY = (upright.height * scale - viewHeight) / 2f
            val left = ((frameRect.left + offsetX) / scale).roundToInt().coerceIn(0, upright.width - 1)
            val top = ((frameRect.top + offsetY) / scale).roundToInt().coerceIn(0, upright.height - 1)
            val right = ((frameRect.right + offsetX) / scale).roundToInt().coerceIn(left + 1, upright.width)
            val bottom = ((frameRect.bottom + offsetY) / scale).roundToInt().coerceIn(top + 1, upright.height)
            Bitmap.createBitmap(upright, left, top, right - left, bottom - top)
        } else {
            upright
        }
        cropped.limitSize().writeJpeg("capture")
    }

    suspend fun fromGallery(uri: Uri): File {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .size(MAX_SIDE)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = (result as? SuccessResult)?.drawable?.toBitmap()
            ?: throw IllegalStateException("Cannot decode $uri")
        return withContext(Dispatchers.Default) { bitmap.limitSize().writeJpeg("gallery") }
    }

    /** Removes files left by earlier scans; the backend keeps its own copy of each upload. */
    suspend fun clearOldFiles() = withContext(Dispatchers.IO) {
        outputDir.listFiles()?.forEach { it.delete() }
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees % 360 == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    private fun Bitmap.limitSize(): Bitmap {
        val longest = max(width, height)
        if (longest <= MAX_SIDE) return this
        val ratio = MAX_SIDE.toFloat() / longest
        return Bitmap.createScaledBitmap(this, (width * ratio).roundToInt(), (height * ratio).roundToInt(), true)
    }

    private fun Bitmap.writeJpeg(prefix: String): File {
        val file = File(outputDir, "${prefix}_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return file
    }

    private companion object {
        const val MAX_SIDE = 1280
        const val JPEG_QUALITY = 85
    }
}
