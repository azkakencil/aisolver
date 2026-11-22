package com.example.overlay

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class ScreenCaptureResult {
  data class Success(val bitmap: Bitmap) : ScreenCaptureResult()

  data class CancelledOrFailed(val reason: String) : ScreenCaptureResult()
}

object ScreenCaptureBus {
  private val _results = MutableSharedFlow<ScreenCaptureResult>(extraBufferCapacity = 1)
  val results: SharedFlow<ScreenCaptureResult> = _results.asSharedFlow()

  fun emitSuccess(bitmap: Bitmap) {
    _results.tryEmit(ScreenCaptureResult.Success(bitmap))
  }

  fun emitFailure(reason: String) {
    _results.tryEmit(ScreenCaptureResult.CancelledOrFailed(reason))
  }
}
