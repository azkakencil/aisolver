package com.example.ui.components

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.ChatMessageUi
import com.example.model.ChatWindowSize
import com.example.model.FloatingPosition
import com.example.model.MessageRole
import com.example.ui.theme.LocalUiDimensions
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun FloatingChatbotOverlay(
  settings: AppSettings,
  isOpen: Boolean,
  windowOverride: ChatWindowSize?,
  messages: List<ChatMessageUi>,
  isGenerating: Boolean,
  streamingAnswer: String,
  chatError: String?,
  onToggleOpen: () -> Unit,
  onClose: () -> Unit,
  onCycleSize: () -> Unit,
  onSendMessage: (String) -> Unit,
  onClearChat: () -> Unit,
  onOpenScanner: () -> Unit,
  onUploadImageUri: (android.net.Uri) -> Unit,
  onRegenerate: (ChatMessageUi) -> Unit,
  onExplain: (String) -> Unit,
  onSummarize: (String) -> Unit,
  onTranslate: (String, String) -> Unit,
  onDismissError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!settings.floatingChatEnabled) return

  val context = LocalContext.current
  val dims = LocalUiDimensions.current
  val effectiveSize = windowOverride ?: settings.chatWindowSize
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  // Drag offset reset whenever user changes floatingPosition in settings
  var dragOffsetX by remember(settings.floatingPosition) { mutableFloatStateOf(0f) }
  var dragOffsetY by remember(settings.floatingPosition) { mutableFloatStateOf(0f) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        onUploadImageUri(uri)
      }
    }

  val voiceLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val spoken =
          result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
          inputText = if (inputText.isBlank()) spoken else "$inputText $spoken"
        }
      }
    }

  // Auto-focus and open keyboard when chatbot opens
  LaunchedEffect(isOpen) {
    if (isOpen) {
      runCatching {
        focusRequester.requestFocus()
        keyboardController?.show()
      }
    }
  }

  // Scroll to bottom when new message or streaming updates
  LaunchedEffect(messages.size, streamingAnswer.length, isGenerating) {
    if (messages.isNotEmpty() || isGenerating) {
      val targetIndex = (messages.size + if (isGenerating) 1 else 0).coerceAtLeast(1) - 1
      runCatching { listState.animateScrollToItem(targetIndex) }
    }
  }

  BoxWithConstraints(
    modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
  ) {
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.toPx() }
    val maxHeightPx = with(density) { maxHeight.toPx() }

    val fabAlignment =
      when (settings.floatingPosition) {
        FloatingPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
        FloatingPosition.BOTTOM_LEFT -> Alignment.BottomStart
        FloatingPosition.MIDDLE_RIGHT -> Alignment.CenterEnd
        FloatingPosition.MIDDLE_LEFT -> Alignment.CenterStart
      }

    // Floating Action Button (Draggable + Positioned + Opacity-controlled)
    if (!isOpen) {
      Box(
        modifier =
          Modifier.align(fabAlignment)
            .padding(
              horizontal = 16.dp,
              vertical =
                if (settings.floatingPosition == FloatingPosition.BOTTOM_RIGHT ||
                  settings.floatingPosition == FloatingPosition.BOTTOM_LEFT
                ) {
                  84.dp
                } else {
                  16.dp
                },
            )
            .offset { IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt()) }
            .pointerInput(settings.floatingPosition) {
              detectDragGestures { change, dragAmount ->
                change.consume()
                dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-maxWidthPx * 0.7f, maxWidthPx * 0.7f)
                dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-maxHeightPx * 0.6f, maxHeightPx * 0.6f)
              }
            }
            .alpha(settings.floatingOpacity),
      ) {
        Surface(
          onClick = onToggleOpen,
          shape = CircleShape,
          color = Color.Transparent,
          modifier =
            Modifier.size(62.dp)
              .shadow(14.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary)
              .testTag("floating_chatbot_fab"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.fillMaxSize()
                .background(
                  Brush.linearGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary,
                      MaterialTheme.colorScheme.secondary,
                    )
                  )
                ),
          ) {
            Icon(
              imageVector = Icons.Filled.SmartToy,
              contentDescription = "Buka Floating AI Chatbot",
              tint = Color.White,
              modifier = Modifier.size(28.dp),
            )
          }
        }
      }
    }

    // Expandable Floating Chatbot Panel
    val panelVisible = isOpen
    val panelHeight = maxHeight * effectiveSize.heightFraction

    AnimatedVisibility(
      visible = panelVisible,
      enter =
        if (settings.chatAnimationEnabled) {
          fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
            scaleIn(initialScale = 0.88f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        } else {
          fadeIn()
        },
      exit =
        if (settings.chatAnimationEnabled) {
          fadeOut() + scaleOut(targetScale = 0.9f)
        } else {
          fadeOut()
        },
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .padding(horizontal = 12.dp, vertical = 12.dp)
          .imePadding(),
    ) {
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = dims.cardSurface.copy(alpha = settings.floatingOpacity.coerceIn(0.82f, 0.99f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        tonalElevation = 10.dp,
        shadowElevation = 18.dp,
        modifier =
          Modifier.widthIn(max = 560.dp)
            .fillMaxWidth()
            .height(panelHeight.coerceIn(280.dp, maxHeight - 24.dp))
            .testTag("floating_chatbot_panel"),
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // Panel Header
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .background(
                  Brush.horizontalGradient(
                    listOf(
                      MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                      MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    )
                  )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.size(36.dp)
                  .background(
                    Brush.linearGradient(
                      listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                      )
                    ),
                    CircleShape,
                  ),
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "AI Solver Assistant",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Ukuran: ${effectiveSize.label} • Claude AI",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            IconButton(
              onClick = onCycleSize,
              modifier = Modifier.size(40.dp).testTag("floating_chat_resize_button"),
            ) {
              Icon(
                imageVector =
                  if (effectiveSize == ChatWindowSize.EXPANDED) {
                    Icons.Filled.CloseFullscreen
                  } else {
                    Icons.Filled.OpenInFull
                  },
                contentDescription = "Ubah ukuran jendela chatbot",
              )
            }
            IconButton(
              onClick = onClearChat,
              modifier = Modifier.size(40.dp).testTag("floating_chat_clear_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.DeleteSweep,
                contentDescription = "Hapus percakapan",
              )
            }
            IconButton(
              onClick = onClose,
              modifier = Modifier.size(40.dp).testTag("floating_chat_close_button"),
            ) {
              Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Tutup panel chatbot",
              )
            }
          }

          // Quick Scan / Upload Shortcut Bar
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              onClick = {
                onClose()
                onOpenScanner()
              },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
              modifier = Modifier.weight(1f).heightIn(min = 38.dp).testTag("floating_chat_scan_btn"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  Icons.Filled.CameraAlt,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Scan Soal", style = MaterialTheme.typography.labelMedium)
              }
            }
            Surface(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
              modifier = Modifier.weight(1f).heightIn(min = 38.dp).testTag("floating_chat_upload_btn"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  Icons.Filled.Image,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("Upload Gambar", style = MaterialTheme.typography.labelMedium)
              }
            }
          }

          // Chat Messages List
          LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            if (messages.isEmpty() && !isGenerating) {
              item {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 8.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(34.dp),
                  )
                  Text(
                    text = "Tanyakan soal apa saja!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text =
                      "Ketik pertanyaan, gunakan suara, scan soal kamera, atau upload foto soal untuk penjelasan langkah demi langkah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            items(messages, key = { it.id }) { msg ->
              FloatingMessageBubble(
                message = msg,
                isGenerating = isGenerating,
                onRegenerate = { onRegenerate(msg) },
                onExplain = { onExplain(msg.content) },
                onSummarize = { onSummarize(msg.content) },
                onTranslate = { lang -> onTranslate(msg.content, lang) },
              )
            }

            if (isGenerating) {
              item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  ThinkingIndicator(animate = settings.chatAnimationEnabled)
                  if (streamingAnswer.isNotBlank()) {
                    Surface(
                      shape = RoundedCornerShape(18.dp),
                      color = dims.elevatedSurface,
                      border = BorderStroke(1.dp, dims.subtleBorder),
                      modifier = Modifier.fillMaxWidth(),
                    ) {
                      FormattedAiText(
                        text = streamingAnswer,
                        modifier = Modifier.padding(12.dp),
                      )
                    }
                  }
                }
              }
            }

            if (chatError != null) {
              item {
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = chatError,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.error,
                      modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismissError) { Text("Tutup") }
                  }
                }
              }
            }
          }

          // Input Bar with Voice & Send
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            OutlinedTextField(
              value = inputText,
              onValueChange = { inputText = it },
              placeholder = { Text("Tulis pertanyaan soal...", maxLines = 1) },
              singleLine = false,
              maxLines = 3,
              shape = RoundedCornerShape(20.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = dims.elevatedSurface,
                  unfocusedContainerColor = dims.elevatedSurface,
                ),
              keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
              keyboardActions =
                KeyboardActions(
                  onSend = {
                    if (inputText.isNotBlank() && !isGenerating) {
                      onSendMessage(inputText)
                      inputText = ""
                    }
                  }
                ),
              modifier =
                Modifier.weight(1f)
                  .focusRequester(focusRequester)
                  .testTag("floating_chat_input"),
            )

            SmallFloatingActionButton(
              onClick = {
                val intent =
                  Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                      RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                      RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan pertanyaan soal Anda")
                  }
                try {
                  voiceLauncher.launch(intent)
                } catch (_: Exception) {
                  Toast.makeText(
                      context,
                      "Input suara tidak tersedia di perangkat ini",
                      Toast.LENGTH_SHORT,
                    )
                    .show()
                }
              },
              containerColor = dims.elevatedSurface,
              contentColor = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(48.dp).testTag("floating_chat_voice_button"),
            ) {
              Icon(Icons.Filled.Mic, contentDescription = "Input suara")
            }

            FilledIconButton(
              onClick = {
                if (inputText.isNotBlank() && !isGenerating) {
                  onSendMessage(inputText)
                  inputText = ""
                }
              },
              enabled = inputText.isNotBlank() && !isGenerating,
              colors =
                IconButtonDefaults.filledIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = Color.White,
                ),
              modifier = Modifier.size(48.dp).testTag("floating_chat_send_button"),
            ) {
              Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim pesan")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun FloatingMessageBubble(
  message: ChatMessageUi,
  isGenerating: Boolean,
  onRegenerate: () -> Unit,
  onExplain: () -> Unit,
  onSummarize: () -> Unit,
  onTranslate: (String) -> Unit,
) {
  val dims = LocalUiDimensions.current
  val isUser = message.role == MessageRole.USER

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
  ) {
    Surface(
      shape =
        RoundedCornerShape(
          topStart = 18.dp,
          topEnd = 18.dp,
          bottomStart = if (isUser) 18.dp else 4.dp,
          bottomEnd = if (isUser) 4.dp else 18.dp,
        ),
      color =
        if (isUser) MaterialTheme.colorScheme.primary else dims.elevatedSurface,
      border = if (isUser) null else BorderStroke(1.dp, dims.subtleBorder),
      modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 1f),
    ) {
      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (isUser) {
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          FormattedAiText(text = message.content)
          AiAnswerActionBar(
            answerText = message.content,
            isBusy = isGenerating,
            onRegenerate = onRegenerate,
            onExplain = onExplain,
            onSummarize = onSummarize,
            onTranslate = onTranslate,
          )
        }
      }
    }
  }
}
