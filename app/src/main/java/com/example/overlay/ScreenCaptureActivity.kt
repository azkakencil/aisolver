package com.example.overlay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class ScreenCaptureActivity : ComponentActivity() {

  private val captureLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      val data = result.data
      if (result.resultCode == Activity.RESULT_OK && data != null) {
        // Finish transparent activity first and wait briefly so the permission dialog
        // disappears completely before capturing the underlying screen.
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        Handler(Looper.getMainLooper())
          .postDelayed(
            {
              val serviceIntent =
                Intent(applicationContext, ScreenCaptureService::class.java).apply {
                  putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                  putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                }
              ContextCompat.startForegroundService(applicationContext, serviceIntent)
            },
            320L,
          )
      } else {
        ScreenCaptureBus.emitFailure("Izin screenshot layar dibatalkan.")
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val projectionManager =
      getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    captureLauncher.launch(projectionManager.createScreenCaptureIntent())
  }
}
