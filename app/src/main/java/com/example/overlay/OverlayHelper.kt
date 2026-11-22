package com.example.overlay

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

object OverlayHelper {
  fun canDrawOverlays(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
  }

  fun openOverlayPermissionSettings(context: Context) {
    val intent =
      Intent(
          Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          Uri.parse("package:${context.packageName}"),
        )
        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    context.startActivity(intent)
  }

  fun startOverlayService(context: Context) {
    if (!canDrawOverlays(context)) return
    val intent = Intent(context, FloatingOverlayService::class.java)
    ContextCompat.startForegroundService(context, intent)
  }

  fun stopOverlayService(context: Context) {
    val intent = Intent(context, FloatingOverlayService::class.java)
    context.stopService(intent)
  }
}
