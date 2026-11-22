package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.AppSettings
import com.example.model.ScanState
import com.example.model.SessionSource
import com.example.model.SubjectTag
import com.example.ui.components.AiAnswerActionBar
import com.example.ui.components.FormattedAiText
import com.example.ui.components.GlassCard
import com.example.ui.components.SubjectSelectorRow
import com.example.ui.components.ThinkingIndicator
import com.example.ui.theme.LocalUiDimensions

@Composable
fun ScannerScreen(
  settings: AppSettings,
  scanState: ScanState,
  croppedPreviewBitmap: Bitmap?,
  autoLaunchGallery: Boolean = false,
  onAutoLaunchGalleryHandled: () -> Unit = {},
  onImageCaptured: (Bitmap) -> Unit,
  onPickGalleryUri: (Uri) -> Unit,
  onOpenCropper: () -> Unit,
  onRunOcrAgain: () -> Unit,
  onUpdateRecognizedText: (String) -> Unit,
  onSelectSubject: (SubjectTag) -> Unit,
  onSolveQuestion: (SessionSource) -> Unit,
  onTransformSolution: (String, String) -> Unit,
  onClearScan: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val dims = LocalUiDimensions.current

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var flashEnabled by remember { mutableStateOf(false) }
  var cameraError by remember { mutableStateOf<String?>(null) }
  var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
  var cameraRef by remember { mutableStateOf<Camera?>(null) }
  var lastSource by remember { mutableStateOf(SessionSource.CAMERA_SCAN) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
    }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        lastSource = SessionSource.GALLERY_SCAN
        onPickGalleryUri(uri)
      }
    }

  LaunchedEffect(autoLaunchGallery) {
    if (autoLaunchGallery) {
      onAutoLaunchGalleryHandled()
      photoPickerLauncher.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
      )
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = dims.spacingMedium, vertical = 12.dp)
        .testTag("scanner_screen"),
    verticalArrangement = Arrangement.spacedBy(dims.spacingMedium),
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "Scan Soal Otomatis",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "KameraX • Crop Presisi • ML Kit OCR • Claude AI",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      if (croppedPreviewBitmap != null || scanState.recognizedText.isNotBlank()) {
        OutlinedButton(
          onClick = onClearScan,
          modifier = Modifier.testTag("scan_reset_button"),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("Scan Baru")
        }
      }
    }

    // Subject selector
    SubjectSelectorRow(selected = scanState.selectedSubject, onSelect = onSelectSubject)

    // CameraX Live Viewfinder OR Cropped Image Result
    if (croppedPreviewBitmap == null) {
      GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(dims.cardCornerRadius))
                .background(Color(0xFF0A0D16)),
            contentAlignment = Alignment.Center,
          ) {
            if (hasCameraPermission) {
              val previewView = remember { PreviewView(context) }

              DisposableEffect(lifecycleOwner, settings.cameraResolution) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val executor = ContextCompat.getMainExecutor(context)
                cameraProviderFuture.addListener(
                  {
                    try {
                      val provider = cameraProviderFuture.get()
                      val targetRes =
                        Size(settings.cameraResolution.width, settings.cameraResolution.height)
                      val resolutionSelector =
                        ResolutionSelector.Builder()
                          .setResolutionStrategy(
                            ResolutionStrategy(
                              targetRes,
                              ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            )
                          )
                          .build()

                      val preview =
                        Preview.Builder().setResolutionSelector(resolutionSelector).build().also {
                          it.surfaceProvider = previewView.surfaceProvider
                        }
                      val capture =
                        ImageCapture.Builder()
                          .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                          .setResolutionSelector(resolutionSelector)
                          .build()

                      provider.unbindAll()
                      val cam =
                        provider.bindToLifecycle(
                          lifecycleOwner,
                          CameraSelector.DEFAULT_BACK_CAMERA,
                          preview,
                          capture,
                        )
                      imageCaptureRef = capture
                      cameraRef = cam
                      cameraError = null
                    } catch (e: Exception) {
                      cameraError = "Kamera tidak tersedia: ${e.localizedMessage ?: "Error"}"
                    }
                  },
                  executor,
                )
                onDispose {
                  runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
                }
              }

              AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
              )

              // Question Framing Guide Overlay
              Box(
                modifier =
                  Modifier.fillMaxWidth(0.86f)
                    .height(170.dp)
                    .border(
                      2.dp,
                      MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                      RoundedCornerShape(16.dp),
                    )
              )

              // Flash Toggle Button
              IconButton(
                onClick = {
                  flashEnabled = !flashEnabled
                  cameraRef?.cameraControl?.enableTorch(flashEnabled)
                },
                modifier =
                  Modifier.align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                    .testTag("camera_flash_button"),
              ) {
                Icon(
                  imageVector = if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                  contentDescription = "Aktifkan/matikan flash",
                  tint = if (flashEnabled) MaterialTheme.colorScheme.secondary else Color.White,
                )
              }

              if (cameraError != null) {
                Surface(
                  color = Color.Black.copy(alpha = 0.78f),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.padding(16.dp),
                ) {
                  Text(
                    text = cameraError!!,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                  )
                }
              }
            } else {
              Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Icon(
                  Icons.Filled.CameraAlt,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(42.dp),
                )
                Text(
                  text = "Izin Kamera Diperlukan",
                  style = MaterialTheme.typography.titleMedium,
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Berikan izin kamera untuk memotret soal langsung, atau pilih gambar dari galeri.",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.75f),
                )
                Button(
                  onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                  modifier = Modifier.heightIn(min = 48.dp).testTag("grant_camera_button"),
                ) {
                  Text("Izinkan Kamera")
                }
              }
            }
          }

          // Primary Camera Controls ("Scan dan Jawab" + Galeri)
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = {
                val capture = imageCaptureRef
                if (!hasCameraPermission) {
                  permissionLauncher.launch(Manifest.permission.CAMERA)
                } else if (capture != null) {
                  lastSource = SessionSource.CAMERA_SCAN
                  capture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                      override fun onCaptureSuccess(image: ImageProxy) {
                        val bmp = imageProxyToBitmap(image)
                        image.close()
                        if (bmp != null) {
                          onImageCaptured(bmp)
                        }
                      }

                      override fun onError(exception: ImageCaptureException) {
                        cameraError = "Gagal mengambil foto: ${exception.localizedMessage}"
                      }
                    },
                  )
                }
              },
              enabled = !scanState.isProcessingOcr,
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.weight(1.4f)
                  .heightIn(min = dims.buttonHeight)
                  .testTag("scan_and_solve_shutter_button"),
            ) {
              if (scanState.isProcessingOcr) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  color = Color.White,
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(Icons.Filled.DocumentScanner, contentDescription = null)
              }
              Spacer(Modifier.width(8.dp))
              Text("Scan dan Jawab")
            }

            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = dims.buttonHeight)
                  .testTag("scan_gallery_button"),
            ) {
              Icon(Icons.Filled.Image, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("Galeri")
            }
          }
        }
      }
    } else {
      // Captured / Cropped Preview Card
      GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Image(
            bitmap = croppedPreviewBitmap.asImageBitmap(),
            contentDescription = "Hasil foto soal terpotong",
            contentScale = ContentScale.Fit,
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(max = 220.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.Black),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedButton(
              onClick = onOpenCropper,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("scan_recrop_button"),
            ) {
              Icon(Icons.Filled.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Potong Ulang")
            }
            FilledTonalButton(
              onClick = onRunOcrAgain,
              enabled = !scanState.isProcessingOcr,
              modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("scan_reocr_button"),
            ) {
              Icon(
                Icons.Filled.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text("Baca Teks OCR")
            }
          }
        }
      }
    }

    // OCR Error Feedback
    if (scanState.ocrError != null) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = scanState.ocrError,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(14.dp),
        )
      }
    }

    // OCR Recognized Text Editor + "Scan dan Jawab" Submit Button
    if (scanState.recognizedText.isNotBlank() || croppedPreviewBitmap != null) {
      GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Teks Soal Terdeteksi (OCR)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
          OutlinedTextField(
            value = scanState.recognizedText,
            onValueChange = onUpdateRecognizedText,
            placeholder = { Text("Hasil pembacaan OCR akan muncul di sini, atau ketik manual...") },
            minLines = 3,
            maxLines = 7,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().testTag("ocr_text_editor"),
          )
          Button(
            onClick = { onSolveQuestion(lastSource) },
            enabled = scanState.recognizedText.isNotBlank() && !scanState.isSolving,
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = dims.buttonHeight)
                .testTag("solve_ocr_button"),
          ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Scan dan Jawab (Langkah demi Langkah)")
          }
        }
      }
    }

    // Thinking / Solve Error / AI Step-by-Step Answer
    if (scanState.isSolving) {
      ThinkingIndicator(
        label = "Menganalisis soal & menyusun langkah penyelesaian...",
        animate = settings.chatAnimationEnabled,
      )
    }

    if (scanState.solveError != null) {
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = scanState.solveError,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(14.dp),
        )
      }
    }

    if (scanState.solutionText.isNotBlank()) {
      GlassCard(modifier = Modifier.fillMaxWidth().testTag("scan_solution_card")) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Filled.AutoAwesome,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
              text = "Pembahasan AI (${scanState.selectedSubject.label})",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }

          FormattedAiText(text = scanState.solutionText)

          AiAnswerActionBar(
            answerText = scanState.solutionText,
            isBusy = scanState.isSolving,
            onRegenerate = { onSolveQuestion(lastSource) },
            onExplain = { onTransformSolution("explain", "") },
            onSummarize = { onTransformSolution("summarize", "") },
            onTranslate = { lang -> onTransformSolution("translate", lang) },
          )
        }
      }
    }

    Spacer(Modifier.height(84.dp))
  }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
  val buffer = image.planes.firstOrNull()?.buffer ?: return null
  val bytes = ByteArray(buffer.remaining())
  buffer.get(bytes)
  val raw = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
  val rotation = image.imageInfo.rotationDegrees
  return if (rotation != 0) {
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
  } else {
    raw
  }
}
