package com.example.overlay

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.R
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

  companion object {
    const val EXTRA_RESULT_CODE = "extra_result_code"
    const val EXTRA_RESULT_DATA = "extra_result_data"
    private const val CHANNEL_ID = "ai_solver_capture_channel"
    private const val NOTIFICATION_ID = 4109
  }

  private var mediaProjection: MediaProjection? = null
  private var virtualDisplay: VirtualDisplay? = null
  private var imageReader: ImageReader? = null
  private var handlerThread: HandlerThread? = null
  private val captured = AtomicBoolean(false)

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
      ?: Activity.RESULT_CANCELED
    @Suppress("DEPRECATION")
    val resultData: Intent? =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
      } else {
        intent?.getParcelableExtra(EXTRA_RESULT_DATA)
      }

    if (resultCode != Activity.RESULT_OK || resultData == null) {
      ScreenCaptureBus.emitFailure("Gagal memperoleh izin screenshot layar.")
      stopSelf()
      return START_NOT_STICKY
    }

    startCaptureForeground()
    captureSingleFrame(resultCode, resultData)
    return START_NOT_STICKY
  }

  private fun startCaptureForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          "Scan Screenshot AI Solver",
          NotificationManager.IMPORTANCE_LOW,
        )
      val nm = getSystemService(NotificationManager::class.java)
      nm.createNotificationChannel(channel)
    }

    val notification: Notification =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Mengambil Screenshot Soal...")
        .setContentText("AI Solver sedang memindai layar untuk membaca soal")
        .setSmallIcon(R.drawable.ic_launcher_art)
        .setOngoing(true)
        .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun captureSingleFrame(resultCode: Int, data: Intent) {
    try {
      val projectionManager =
        getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
      val projection = projectionManager.getMediaProjection(resultCode, data)
      if (projection == null) {
        ScreenCaptureBus.emitFailure("MediaProjection tidak tersedia.")
        cleanupAndStop()
        return
      }
      mediaProjection = projection

      val (screenW, screenH, densityDpi) = resolveDisplayBounds()
      val scale = if (screenW > 1280) 1280f / screenW.toFloat() else 1f
      val capW = (screenW * scale).toInt().coerceAtLeast(360)
      val capH = (screenH * scale).toInt().coerceAtLeast(640)

      val thread = HandlerThread("AiSolverCaptureThread").apply { start() }
      handlerThread = thread
      val bgHandler = Handler(thread.looper)

      projection.registerCallback(
        object : MediaProjection.Callback() {
          override fun onStop() {
            if (captured.compareAndSet(false, true)) {
              ScreenCaptureBus.emitFailure("Sesi screenshot dihentikan sistem.")
              cleanupAndStop()
            }
          }
        },
        bgHandler,
      )

      val reader = ImageReader.newInstance(capW, capH, PixelFormat.RGBA_8888, 2)
      imageReader = reader

      reader.setOnImageAvailableListener(
        { r ->
          if (captured.get()) return@setOnImageAvailableListener
          val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
          try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * capW

            val paddedBitmap =
              Bitmap.createBitmap(
                capW + rowPadding / pixelStride,
                capH,
                Bitmap.Config.ARGB_8888,
              )
            paddedBitmap.copyPixelsFromBuffer(buffer)
            val finalBitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, capW, capH)
            if (finalBitmap != paddedBitmap) {
              paddedBitmap.recycle()
            }
            if (captured.compareAndSet(false, true)) {
              ScreenCaptureBus.emitSuccess(finalBitmap)
              cleanupAndStop()
            } else {
              finalBitmap.recycle()
            }
          } catch (e: Exception) {
            if (captured.compareAndSet(false, true)) {
              ScreenCaptureBus.emitFailure("Gagal memproses gambar layar: ${e.localizedMessage}")
              cleanupAndStop()
            }
          } finally {
            image.close()
          }
        },
        bgHandler,
      )

      virtualDisplay =
        projection.createVirtualDisplay(
          "AiSolverScreenCapture",
          capW,
          capH,
          densityDpi,
          DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
          reader.surface,
          null,
          bgHandler,
        )

      // Timeout safeguard if the screen didn't emit a new frame within 3s
      Handler(Looper.getMainLooper())
        .postDelayed(
          {
            if (captured.compareAndSet(false, true)) {
              ScreenCaptureBus.emitFailure("Waktu tunggu screenshot habis. Coba lagi.")
              cleanupAndStop()
            }
          },
          3000L,
        )
    } catch (e: Exception) {
      if (captured.compareAndSet(false, true)) {
        ScreenCaptureBus.emitFailure("Tidak dapat mengambil screenshot: ${e.localizedMessage}")
        cleanupAndStop()
      }
    }
  }

  private fun resolveDisplayBounds(): Triple<Int, Int, Int> {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val density = resources.displayMetrics.densityDpi
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val bounds = wm.currentWindowMetrics.bounds
      Triple(bounds.width(), bounds.height(), density)
    } else {
      val metrics = DisplayMetrics()
      @Suppress("DEPRECATION")
      wm.defaultDisplay.getRealMetrics(metrics)
      Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
    }
  }

  private fun cleanupAndStop() {
    runCatching { virtualDisplay?.release() }
    virtualDisplay = null
    runCatching { imageReader?.setOnImageAvailableListener(null, null) }
    runCatching { imageReader?.close() }
    imageReader = null
    runCatching { mediaProjection?.stop() }
    mediaProjection = null
    runCatching { handlerThread?.quitSafely() }
    handlerThread = null
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  override fun onDestroy() {
    runCatching { virtualDisplay?.release() }
    runCatching { imageReader?.close() }
    runCatching { mediaProjection?.stop() }
    runCatching { handlerThread?.quitSafely() }
    super.onDestroy()
  }
}
