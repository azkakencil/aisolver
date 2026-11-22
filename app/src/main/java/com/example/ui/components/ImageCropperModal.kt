package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlin.math.hypot

private enum class DragHandle {
  TOP_LEFT,
  TOP_RIGHT,
  BOTTOM_LEFT,
  BOTTOM_RIGHT,
  MOVE,
  NONE,
}

@Composable
fun ImageCropperModal(
  bitmap: Bitmap,
  initialRect: RectF,
  onConfirmCrop: (RectF) -> Unit,
  onCancel: () -> Unit,
) {
  var left by remember(bitmap) { mutableFloatStateOf(initialRect.left.coerceIn(0.02f, 0.8f)) }
  var top by remember(bitmap) { mutableFloatStateOf(initialRect.top.coerceIn(0.02f, 0.8f)) }
  var right by remember(bitmap) { mutableFloatStateOf(initialRect.right.coerceIn(left + 0.15f, 0.98f)) }
  var bottom by remember(bitmap) { mutableFloatStateOf(initialRect.bottom.coerceIn(top + 0.15f, 0.98f)) }
  var activeHandle by remember { mutableStateOf(DragHandle.NONE) }

  val primary = MaterialTheme.colorScheme.primary
  val secondary = MaterialTheme.colorScheme.secondary

  Surface(
    color = Color(0xFF0A0C12),
    modifier = Modifier.fillMaxSize().testTag("image_cropper_modal"),
  ) {
    Column(
      modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButton(onClick = onCancel, modifier = Modifier.testTag("crop_cancel_icon")) {
          Icon(Icons.Filled.Close, contentDescription = "Batal potong", tint = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "Potong Area Soal",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
          )
          Text(
            text = "Seret sudut bingkai untuk memfokuskan soal",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
          )
        }
        Icon(
          imageVector = Icons.Filled.Crop,
          contentDescription = null,
          tint = secondary,
          modifier = Modifier.size(24.dp),
        )
      }

      BoxWithConstraints(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        val density = LocalDensity.current
        val containerW = with(density) { maxWidth.toPx() }
        val containerH = with(density) { maxHeight.toPx() }

        val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
        val boxAspect = containerW / containerH.coerceAtLeast(1f)

        val (renderW, renderH) =
          if (imgAspect > boxAspect) {
            containerW to (containerW / imgAspect)
          } else {
            (containerH * imgAspect) to containerH
          }

        val renderWDp = with(density) { renderW.toDp() }
        val renderHDp = with(density) { renderH.toDp() }

        Box(
          modifier = Modifier.size(renderWDp, renderHDp).background(Color.Black),
        ) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Pratinjau gambar untuk dipotong",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )

          Canvas(
            modifier =
              Modifier.fillMaxSize().pointerInput(renderW, renderH) {
                detectDragGestures(
                  onDragStart = { pos ->
                    val l = left * renderW
                    val t = top * renderH
                    val r = right * renderW
                    val b = bottom * renderH
                    val hitRadius = 70f
                    activeHandle =
                      when {
                        hypot(pos.x - l, pos.y - t) < hitRadius -> DragHandle.TOP_LEFT
                        hypot(pos.x - r, pos.y - t) < hitRadius -> DragHandle.TOP_RIGHT
                        hypot(pos.x - l, pos.y - b) < hitRadius -> DragHandle.BOTTOM_LEFT
                        hypot(pos.x - r, pos.y - b) < hitRadius -> DragHandle.BOTTOM_RIGHT
                        pos.x in l..r && pos.y in t..b -> DragHandle.MOVE
                        else -> DragHandle.NONE
                      }
                  },
                  onDragEnd = { activeHandle = DragHandle.NONE },
                  onDragCancel = { activeHandle = DragHandle.NONE },
                  onDrag = { change, dragAmount ->
                    change.consume()
                    val dx = dragAmount.x / renderW.coerceAtLeast(1f)
                    val dy = dragAmount.y / renderH.coerceAtLeast(1f)
                    val minSpan = 0.12f
                    when (activeHandle) {
                      DragHandle.TOP_LEFT -> {
                        left = (left + dx).coerceIn(0f, right - minSpan)
                        top = (top + dy).coerceIn(0f, bottom - minSpan)
                      }
                      DragHandle.TOP_RIGHT -> {
                        right = (right + dx).coerceIn(left + minSpan, 1f)
                        top = (top + dy).coerceIn(0f, bottom - minSpan)
                      }
                      DragHandle.BOTTOM_LEFT -> {
                        left = (left + dx).coerceIn(0f, right - minSpan)
                        bottom = (bottom + dy).coerceIn(top + minSpan, 1f)
                      }
                      DragHandle.BOTTOM_RIGHT -> {
                        right = (right + dx).coerceIn(left + minSpan, 1f)
                        bottom = (bottom + dy).coerceIn(top + minSpan, 1f)
                      }
                      DragHandle.MOVE -> {
                        val w = right - left
                        val h = bottom - top
                        val newL = (left + dx).coerceIn(0f, 1f - w)
                        val newT = (top + dy).coerceIn(0f, 1f - h)
                        left = newL
                        top = newT
                        right = newL + w
                        bottom = newT + h
                      }
                      DragHandle.NONE -> Unit
                    }
                  },
                )
              },
          ) {
            val l = left * size.width
            val t = top * size.height
            val r = right * size.width
            val b = bottom * size.height
            val w = r - l
            val h = b - t

            // Darken outside regions
            val scrim = Color.Black.copy(alpha = 0.58f)
            drawRect(scrim, Offset.Zero, Size(size.width, t))
            drawRect(scrim, Offset(0f, b), Size(size.width, size.height - b))
            drawRect(scrim, Offset(0f, t), Size(l, h))
            drawRect(scrim, Offset(r, t), Size(size.width - r, h))

            // Crop border
            drawRect(
              color = secondary,
              topLeft = Offset(l, t),
              size = Size(w, h),
              style = Stroke(width = 3.dp.toPx()),
            )

            // Grid guide lines
            val guideColor = Color.White.copy(alpha = 0.35f)
            drawLine(guideColor, Offset(l + w / 3f, t), Offset(l + w / 3f, b), strokeWidth = 1.dp.toPx())
            drawLine(guideColor, Offset(l + 2f * w / 3f, t), Offset(l + 2f * w / 3f, b), strokeWidth = 1.dp.toPx())
            drawLine(guideColor, Offset(l, t + h / 3f), Offset(r, t + h / 3f), strokeWidth = 1.dp.toPx())
            drawLine(guideColor, Offset(l, t + 2f * h / 3f), Offset(r, t + 2f * h / 3f), strokeWidth = 1.dp.toPx())

            // Corner handles
            val handleRadius = 11.dp.toPx()
            listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { pt ->
              drawCircle(color = primary, radius = handleRadius, center = pt)
              drawCircle(color = Color.White, radius = handleRadius * 0.55f, center = pt)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        OutlinedButton(
          onClick = {
            left = 0.02f
            top = 0.02f
            right = 0.98f
            bottom = 0.98f
          },
          modifier = Modifier.weight(1f).height(50.dp).testTag("crop_full_button"),
        ) {
          Text("Pilih Semua", color = Color.White)
        }
        Button(
          onClick = { onConfirmCrop(RectF(left, top, right, bottom)) },
          modifier = Modifier.weight(1.4f).height(50.dp).testTag("crop_confirm_button"),
        ) {
          Text("Potong & Baca Teks")
        }
      }
    }
  }
}
