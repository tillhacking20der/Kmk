package com.example.service

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

/**
 * ScreenCaptureManager handles Android's official MediaProjection screen capture.
 *
 * Strict Privacy Protections:
 * 1. Requires explicit user approval via Android's MediaProjection permission intent.
 * 2. Never captures when screen is locked (Keyguard locked).
 * 3. Never saves recordings or frames to disk (in-memory only).
 * 4. Only captures a single frame on-demand when the user asks a screen-related question.
 * 5. Promptly releases all resources (VirtualDisplay, ImageReader, MediaProjection) when disabled.
 */
object ScreenCaptureManager {

    private const val TAG = "ScreenCaptureManager"

    private val _isScreenCaptureActive = MutableStateFlow(false)
    val isScreenCaptureActive: StateFlow<Boolean> = _isScreenCaptureActive.asStateFlow()

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var displayWidth: Int = 720
    private var displayHeight: Int = 1280
    private var displayDensity: Int = DisplayMetrics.DENSITY_DEFAULT

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection stopped by system")
            stop()
        }
    }

    @Synchronized
    fun start(context: Context, resultCode: Int, data: Intent): Boolean {
        try {
            stop() // Clean up any existing session

            val mpManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
                ?: return false

            val projection = mpManager.getMediaProjection(resultCode, data) ?: return false
            mediaProjection = projection

            // Determine dimensions scaled appropriately for AI efficiency (max width ~720)
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay?.getRealMetrics(metrics)

            val rawWidth = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
            val rawHeight = if (metrics.heightPixels > 0) metrics.heightPixels else 2400
            val scale = 720f / rawWidth.coerceAtLeast(1)

            displayWidth = (rawWidth * scale).toInt().coerceAtLeast(360)
            displayHeight = (rawHeight * scale).toInt().coerceAtLeast(640)
            displayDensity = if (metrics.densityDpi > 0) metrics.densityDpi else DisplayMetrics.DENSITY_DEFAULT

            // Initialize ImageReader for frame capture
            val reader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            projection.registerCallback(projectionCallback, mainHandler)

            virtualDisplay = projection.createVirtualDisplay(
                "JarvisScreenCapture",
                displayWidth,
                displayHeight,
                displayDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )

            _isScreenCaptureActive.value = true
            Log.i(TAG, "Screen capture initiated successfully: ${displayWidth}x${displayHeight}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ScreenCaptureManager", e)
            stop()
            return false
        }
    }

    @Synchronized
    fun stop() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null

            imageReader?.close()
            imageReader = null

            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
            mediaProjection = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping ScreenCaptureManager", e)
        } finally {
            _isScreenCaptureActive.value = false
            Log.i(TAG, "Screen capture stopped and resources released")
        }
    }

    /**
     * Captures a single frame strictly in-memory if screen context is active and unlocked.
     * Returns null if screen context is not active, if screen is locked, or if error occurs.
     */
    @Synchronized
    fun captureCurrentFrame(context: Context): Bitmap? {
        if (!_isScreenCaptureActive.value) {
            Log.d(TAG, "Screen capture is not active")
            return null
        }

        // Privacy rule 6: Never capture the lock screen
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager?.isKeyguardLocked == true) {
            Log.w(TAG, "Refusing screen capture: device is locked")
            return null
        }

        val reader = imageReader ?: return null
        var image: android.media.Image? = null

        try {
            image = reader.acquireLatestImage()
            if (image == null) {
                // Retry once briefly if first frame hasn't arrived
                Thread.sleep(100)
                image = reader.acquireLatestImage()
            }
            if (image == null) return null

            val planes = image.planes
            if (planes.isEmpty()) return null

            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * displayWidth

            val bitmap = Bitmap.createBitmap(
                displayWidth + rowPadding / pixelStride,
                displayHeight,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop out row padding if necessary
            val finalBitmap = if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, displayWidth, displayHeight)
            } else {
                bitmap
            }

            return finalBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to capture screen frame", e)
            return null
        } finally {
            image?.close()
        }
    }
}
