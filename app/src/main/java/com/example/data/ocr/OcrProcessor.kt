package com.example.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import com.example.model.ScanQuality
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

class OcrProcessor(private val context: Context) {

  private val recognizer by lazy {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
  }

  suspend fun decodeBitmapFromUri(uri: Uri, quality: ScanQuality): Bitmap =
    withContext(Dispatchers.IO) {
      val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
      }

      val maxDim = quality.maxDimensionPx
      val sampleSize = calculateInSampleSize(boundsOptions, maxDim, maxDim)
      val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

      val rawBitmap =
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: throw IllegalStateException("Gagal membaca file gambar.")

      val rotated = applyExifOrientation(uri, rawBitmap)
      scaleBitmapIfNeeded(rotated, maxDim)
    }

  suspend fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap =
    withContext(Dispatchers.Default) {
      val w = bitmap.width
      val h = bitmap.height
      val largest = max(w, h)
      if (largest <= maxDimension) return@withContext bitmap
      val ratio = maxDimension.toFloat() / largest.toFloat()
      val newW = (w * ratio).roundToInt().coerceAtLeast(1)
      val newH = (h * ratio).roundToInt().coerceAtLeast(1)
      Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

  suspend fun cropBitmapNormalized(bitmap: Bitmap, normalizedRect: RectF): Bitmap =
    withContext(Dispatchers.Default) {
      val left = (normalizedRect.left.coerceIn(0f, 0.95f) * bitmap.width).roundToInt()
      val top = (normalizedRect.top.coerceIn(0f, 0.95f) * bitmap.height).roundToInt()
      val right =
        (normalizedRect.right.coerceIn(normalizedRect.left + 0.05f, 1f) * bitmap.width)
          .roundToInt()
          .coerceAtMost(bitmap.width)
      val bottom =
        (normalizedRect.bottom.coerceIn(normalizedRect.top + 0.05f, 1f) * bitmap.height)
          .roundToInt()
          .coerceAtMost(bitmap.height)

      val cropWidth = (right - left).coerceAtLeast(1)
      val cropHeight = (bottom - top).coerceAtLeast(1)
      Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

  /**
   * Computes a smart normalized crop bounding box around detected text blocks when Auto-Crop is
   * enabled, falling back to a clean central focus frame.
   */
  suspend fun detectSuggestedCropRect(bitmap: Bitmap): RectF {
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    val visionText = recognizer.process(inputImage).awaitResult()
    val boxes = visionText.textBlocks.mapNotNull { it.boundingBox }
    if (boxes.isEmpty()) {
      return RectF(0.08f, 0.15f, 0.92f, 0.85f)
    }
    var minX = Int.MAX_VALUE
    var minY = Int.MAX_VALUE
    var maxX = 0
    var maxY = 0
    boxes.forEach { box ->
      minX = minOf(minX, box.left)
      minY = minOf(minY, box.top)
      maxX = maxOf(maxX, box.right)
      maxY = maxOf(maxY, box.bottom)
    }
    val padX = (bitmap.width * 0.04f)
    val padY = (bitmap.height * 0.04f)
    val left = ((minX - padX) / bitmap.width).coerceIn(0.02f, 0.9f)
    val top = ((minY - padY) / bitmap.height).coerceIn(0.02f, 0.9f)
    val right = ((maxX + padX) / bitmap.width).coerceIn(left + 0.08f, 0.98f)
    val bottom = ((maxY + padY) / bitmap.height).coerceIn(top + 0.08f, 0.98f)
    return RectF(left, top, right, bottom)
  }

  suspend fun recognizeText(bitmap: Bitmap): String {
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    val result = recognizer.process(inputImage).awaitResult()
    return result.text.trim()
  }

  private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
    var inputStream: InputStream? = null
    return try {
      inputStream = context.contentResolver.openInputStream(uri)
      if (inputStream == null) return bitmap
      val exif = ExifInterface(inputStream)
      val orientation =
        exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
      val degrees =
        when (orientation) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90f
          ExifInterface.ORIENTATION_ROTATE_180 -> 180f
          ExifInterface.ORIENTATION_ROTATE_270 -> 270f
          else -> 0f
        }
      if (degrees == 0f) {
        bitmap
      } else {
        val matrix = Matrix().apply { postRotate(degrees) }
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      }
    } catch (_: Exception) {
      bitmap
    } finally {
      runCatching { inputStream?.close() }
    }
  }

  private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
  ): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
      val halfHeight = height / 2
      val halfWidth = width / 2
      while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
        inSampleSize *= 2
      }
    }
    return inSampleSize
  }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
  addOnSuccessListener { res -> if (cont.isActive) cont.resume(res) }
  addOnFailureListener { err -> if (cont.isActive) cont.resumeWithException(err) }
  addOnCanceledListener { if (cont.isActive) cont.cancel() }
}
