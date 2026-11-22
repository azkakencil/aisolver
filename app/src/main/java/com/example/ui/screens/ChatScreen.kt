package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.model.AppSettings
import com.example.model.ChatMessageUi
import com.example.model.MessageRole
import com.example.model.SubjectTag
import com.example.ui.components.AiAnswerActionBar
import com.example.ui.components.FormattedAiText
import com.example.ui.components.GlassCard
import com.example.ui.components.SubjectSelectorRow
import com.example.ui.components.ThinkingIndicator
import com.example.ui.theme.LocalUiDimensions
import java.util.Locale

@Composable
fun ChatScreen(
  settings: AppSettings,
  messages: List<ChatMessageUi>,
  isGenerating: Boolean,
  streamingAnswer: String,
  chatError: String?,
  autoFocusInput: Boolean,
  onSendMessage: (String, SubjectTag) -> Unit,
  onNewChat: () -> Unit,
  onClearChat: () -> Unit,
  onOpenScanner: () -> Unit,
  onUploadImageUri: (Uri) -> Unit,
  onRegenerate: (ChatMessageUi) -> Unit,
  onExplain: (String) -> Unit,
  onSummarize: (String) -> Unit,
  onTranslate: (String, String) -> Unit,
  onDismissError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val dims = LocalUiDimensions.current
  var inputText by remember { mutableStateOf("") }
  var selectedSubject by remember { mutableStateOf(SubjectTag.ALL) }
  val listState = rememberLazyListState()
  val focusRequester = remember { FocusRequester() }
  val keyboardController = LocalSoftwareKeyboardController.current

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) onUploadImageUri(uri)
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

  LaunchedEffect(autoFocusInput) {
    if (autoFocusInput) {
      runCatching {
        focusRequester.requestFocus()
        keyboardController?.show()
      }
    }
  }

  LaunchedEffect(messages.size, streamingAnswer.length, isGenerating) {
    if (messages.isNotEmpty() || isGenerating) {
      val idx = (messages.size + if (isGenerating) 1 else 0).coerceAtLeast(1) - 1
      runCatching { listState.animateScrollToItem(idx) }
    }
  }

  Column(
    modifier = modifier.fillMaxSize().imePadding().testTag("chat_screen"),
  ) {
    // Chat Header
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = dims.spacingMedium, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text(
          text = "Chat AI Solver",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Diskusi soal & penjelasan otomatis",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Row {
        IconButton(onClick = onNewChat, modifier = Modifier.testTag("chat_new_button")) {
          Icon(Icons.Filled.AddComment, contentDescription = "Percakapan baru")
        }
        IconButton(onClick = onClearChat, modifier = Modifier.testTag("chat_clear_button")) {
          Icon(Icons.Filled.DeleteSweep, contentDescription = "Hapus percakapan")
        }
      }
    }

    SubjectSelectorRow(
      selected = selectedSubject,
      onSelect = { selectedSubject = it },
      modifier = Modifier.padding(horizontal = dims.spacingMedium),
    )

    Spacer(Modifier.height(6.dp))

    // Messages Feed
    LazyColumn(
      state = listState,
      modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = dims.spacingMedium),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (messages.isEmpty() && !isGenerating) {
        item {
          GlassCard(modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp),
              )
              Text(
                text = "Siap Membantu Tugas & Soalmu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "Ketik pertanyaan di bawah, gunakan mikrofon, scan lewat kamera, atau upload foto soal dari galeri.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      items(messages, key = { it.id }) { msg ->
        val isUser = msg.role == MessageRole.USER
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
          Surface(
            shape =
              RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 6.dp,
                bottomEnd = if (isUser) 6.dp else 20.dp,
              ),
            color =
              if (isUser) MaterialTheme.colorScheme.primary else dims.cardSurface,
            border = if (isUser) null else BorderStroke(1.dp, dims.subtleBorder),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 1f),
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              if (isUser) {
                Text(
                  text = msg.content,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                FormattedAiText(text = msg.content)
                AiAnswerActionBar(
                  answerText = msg.content,
                  isBusy = isGenerating,
                  onRegenerate = { onRegenerate(msg) },
                  onExplain = { onExplain(msg.content) },
                  onSummarize = { onSummarize(msg.content) },
                  onTranslate = { lang -> onTranslate(msg.content, lang) },
                )
              }
            }
          }
        }
      }

      if (isGenerating) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ThinkingIndicator(animate = settings.chatAnimationEnabled)
            if (streamingAnswer.isNotBlank()) {
              GlassCard(modifier = Modifier.fillMaxWidth()) {
                FormattedAiText(
                  text = streamingAnswer,
                  modifier = Modifier.padding(14.dp),
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

      item { Spacer(Modifier.height(12.dp)) }
    }

    // Input Toolbar
    Surface(
      color = dims.cardSurface,
      border = BorderStroke(1.dp, dims.subtleBorder),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          IconButton(
            onClick = onOpenScanner,
            modifier = Modifier.size(44.dp).testTag("chat_scan_shortcut"),
          ) {
            Icon(
              Icons.Filled.CameraAlt,
              contentDescription = "Scan soal",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          IconButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.size(44.dp).testTag("chat_upload_shortcut"),
          ) {
            Icon(
              Icons.Filled.Image,
              contentDescription = "Upload gambar soal",
              tint = MaterialTheme.colorScheme.secondary,
            )
          }

          OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Tanya AI Solver...") },
            maxLines = 4,
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
                    onSendMessage(inputText, selectedSubject)
                    inputText = ""
                  }
                }
              ),
            modifier =
              Modifier.weight(1f)
                .focusRequester(focusRequester)
                .testTag("chat_screen_input"),
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
                }
              try {
                voiceLauncher.launch(intent)
              } catch (_: Exception) {
                Toast.makeText(
                    context,
                    "Input suara tidak didukung perangkat ini",
                    Toast.LENGTH_SHORT,
                  )
                  .show()
              }
            },
            containerColor = dims.elevatedSurface,
            contentColor = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(46.dp).testTag("chat_voice_button"),
          ) {
            Icon(Icons.Filled.Mic, contentDescription = "Input suara")
          }

          FilledIconButton(
            onClick = {
              if (inputText.isNotBlank() && !isGenerating) {
                onSendMessage(inputText, selectedSubject)
                inputText = ""
              }
            },
            enabled = inputText.isNotBlank() && !isGenerating,
            colors =
              IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
              ),
            modifier = Modifier.size(46.dp).testTag("chat_send_button"),
          ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Kirim")
          }
        }
      }
    }
  }
}
