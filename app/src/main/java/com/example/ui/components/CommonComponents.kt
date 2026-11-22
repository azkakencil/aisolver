package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.model.ApiConnectionStatus
import com.example.model.SubjectTag
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalUiDimensions
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.WarningAmber

@Composable
fun ApiStatusBadge(
  status: ApiConnectionStatus,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bgColor by
    animateColorAsState(
      targetValue =
        when (status) {
          is ApiConnectionStatus.Online -> SuccessMint.copy(alpha = 0.16f)
          is ApiConnectionStatus.Checking -> WarningAmber.copy(alpha = 0.16f)
          is ApiConnectionStatus.Offline -> ErrorCoral.copy(alpha = 0.16f)
        },
      label = "apiStatusBg",
    )
  val contentColor =
    when (status) {
      is ApiConnectionStatus.Online -> SuccessMint
      is ApiConnectionStatus.Checking -> WarningAmber
      is ApiConnectionStatus.Offline -> ErrorCoral
    }
  val label =
    when (status) {
      is ApiConnectionStatus.Online -> "API Online • ${status.latencyMs}ms"
      is ApiConnectionStatus.Checking -> "Memeriksa API..."
      is ApiConnectionStatus.Offline -> "API Offline"
    }
  val icon =
    when (status) {
      is ApiConnectionStatus.Online -> Icons.Filled.CheckCircle
      is ApiConnectionStatus.Checking -> Icons.Filled.Sync
      is ApiConnectionStatus.Offline -> Icons.Filled.CloudOff
    }

  Surface(
    onClick = onClick,
    shape = CircleShape,
    color = bgColor,
    border = BorderStroke(1.dp, contentColor.copy(alpha = 0.4f)),
    modifier = modifier.heightIn(min = 36.dp).testTag("api_status_badge"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = contentColor,
        modifier = Modifier.size(14.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = contentColor,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val dims = LocalUiDimensions.current
  val shape = RoundedCornerShape(dims.cardCornerRadius)
  val border = BorderStroke(1.dp, dims.subtleBorder.copy(alpha = if (dims.isDark) 0.7f else 0.9f))
  val elevation = if (dims.isDark) 2.dp else 4.dp

  if (onClick != null) {
    Surface(
      onClick = onClick,
      shape = shape,
      color = dims.cardSurface,
      border = border,
      tonalElevation = elevation,
      shadowElevation = if (dims.isDark) 0.dp else 3.dp,
      modifier = modifier,
      content = content,
    )
  } else {
    Surface(
      shape = shape,
      color = dims.cardSurface,
      border = border,
      tonalElevation = elevation,
      shadowElevation = if (dims.isDark) 0.dp else 3.dp,
      modifier = modifier,
      content = content,
    )
  }
}

@Composable
fun ThinkingIndicator(
  label: String = "AI sedang berpikir...",
  animate: Boolean = true,
  modifier: Modifier = Modifier,
) {
  val transition = rememberInfiniteTransition(label = "thinking")
  val dot1 by
    transition.animateFloat(
      initialValue = 0.25f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
      label = "d1",
    )
  val dot2 by
    transition.animateFloat(
      initialValue = 0.25f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(tween(500, delayMillis = 160, easing = LinearEasing), RepeatMode.Reverse),
      label = "d2",
    )
  val dot3 by
    transition.animateFloat(
      initialValue = 0.25f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(tween(500, delayMillis = 320, easing = LinearEasing), RepeatMode.Reverse),
      label = "d3",
    )

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    modifier = modifier.testTag("ai_thinking_indicator"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.AutoAwesome,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp),
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(if (animate) dot1 else 1f, if (animate) dot2 else 0.7f, if (animate) dot3 else 0.5f)
          .forEach { alpha ->
            Box(
              modifier =
                Modifier.size(7.dp)
                  .alpha(alpha)
                  .clip(CircleShape)
                  .background(
                    Brush.horizontalGradient(
                      listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                      )
                    )
                  )
            )
          }
      }
    }
  }
}

@Composable
fun SubjectSelectorRow(
  selected: SubjectTag,
  onSelect: (SubjectTag) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    SubjectTag.entries.forEach { tag ->
      val isSelected = tag == selected
      FilterChip(
        selected = isSelected,
        onClick = { onSelect(tag) },
        label = {
          Text(
            text = tag.label,
            style = MaterialTheme.typography.labelMedium,
          )
        },
        colors =
          FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
          ),
        modifier = Modifier.testTag("subject_chip_${tag.name.lowercase()}"),
      )
    }
  }
}

@Composable
fun FormattedAiText(
  text: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onSurface,
) {
  val blocks = remember(text) { text.split("```") }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    blocks.forEachIndexed { index, segment ->
      if (index % 2 == 1) {
        // Code / Formula block
        val cleaned = segment.substringAfter('\n', segment).trim()
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = cleaned,
            fontFamily = JetBrainsMonoFontFamily,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(12.dp),
          )
        }
      } else if (segment.isNotBlank()) {
        val annotated = remember(segment) { parseSimpleBold(segment.trim()) }
        Text(
          text = annotated,
          style = MaterialTheme.typography.bodyMedium,
          color = color,
        )
      }
    }
  }
}

private fun parseSimpleBold(raw: String) = buildAnnotatedString {
  var idx = 0
  while (idx < raw.length) {
    val start = raw.indexOf("**", idx)
    if (start == -1) {
      append(raw.substring(idx))
      break
    }
    append(raw.substring(idx, start))
    val end = raw.indexOf("**", start + 2)
    if (end == -1) {
      append(raw.substring(start))
      break
    }
    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
      append(raw.substring(start + 2, end))
    }
    idx = end + 2
  }
}

@Composable
fun AiAnswerActionBar(
  answerText: String,
  onRegenerate: () -> Unit,
  onExplain: () -> Unit,
  onSummarize: () -> Unit,
  onTranslate: (String) -> Unit,
  modifier: Modifier = Modifier,
  isBusy: Boolean = false,
) {
  val context = LocalContext.current
  var showTranslateDialog by remember { mutableStateOf(false) }

  if (showTranslateDialog) {
    TranslateLanguageDialog(
      onSelectLanguage = { lang ->
        showTranslateDialog = false
        onTranslate(lang)
      },
      onDismiss = { showTranslateDialog = false },
    )
  }

  Row(
    modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    ActionPillChip(
      label = "Salin",
      icon = Icons.Filled.ContentCopy,
      enabled = answerText.isNotBlank(),
      testTag = "action_copy_answer",
      onClick = { copyTextToClipboard(context, answerText) },
    )
    ActionPillChip(
      label = "Bagikan",
      icon = Icons.Filled.Share,
      enabled = answerText.isNotBlank(),
      testTag = "action_share_answer",
      onClick = { shareText(context, answerText) },
    )
    ActionPillChip(
      label = "Regenerate",
      icon = Icons.Filled.Refresh,
      enabled = !isBusy,
      testTag = "action_regenerate_answer",
      onClick = onRegenerate,
    )
    ActionPillChip(
      label = "Jelaskan",
      icon = Icons.Filled.Psychology,
      enabled = !isBusy && answerText.isNotBlank(),
      testTag = "action_explain_answer",
      onClick = onExplain,
    )
    ActionPillChip(
      label = "Ringkas",
      icon = Icons.AutoMirrored.Filled.ShortText,
      enabled = !isBusy && answerText.isNotBlank(),
      testTag = "action_summarize_answer",
      onClick = onSummarize,
    )
    ActionPillChip(
      label = "Bahasa",
      icon = Icons.Filled.Translate,
      enabled = !isBusy && answerText.isNotBlank(),
      testTag = "action_translate_answer",
      onClick = { showTranslateDialog = true },
    )
  }
}

@Composable
private fun ActionPillChip(
  label: String,
  icon: ImageVector,
  enabled: Boolean,
  testTag: String,
  onClick: () -> Unit,
) {
  AssistChip(
    onClick = onClick,
    enabled = enabled,
    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
    leadingIcon = {
      Icon(
        imageVector = icon,
        contentDescription = label,
        modifier = Modifier.size(15.dp),
      )
    },
    colors =
      AssistChipDefaults.assistChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        labelColor = MaterialTheme.colorScheme.onSurface,
        leadingIconContentColor = MaterialTheme.colorScheme.secondary,
      ),
    modifier = Modifier.testTag(testTag),
  )
}

@Composable
fun TranslateLanguageDialog(
  onSelectLanguage: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val languages = listOf("Indonesia", "Inggris", "Arab", "Jepang", "Mandarin", "Jerman")
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Ubah Bahasa Jawaban") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "Pilih bahasa tujuan untuk menerjemahkan jawaban AI:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        languages.forEach { lang ->
          Surface(
            onClick = { onSelectLanguage(lang) },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
          ) {
            Box(
              contentAlignment = Alignment.CenterStart,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
              Text(text = "Bahasa $lang", style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Tutup") }
    },
  )
}

fun copyTextToClipboard(context: Context, text: String) {
  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  cm.setPrimaryClip(ClipData.newPlainText("AI Solver Answer", text))
  Toast.makeText(context, "Jawaban disalin ke clipboard", Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
  val intent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, "Jawaban AI Solver")
      putExtra(Intent.EXTRA_TEXT, text)
    }
  context.startActivity(Intent.createChooser(intent, "Bagikan jawaban AI"))
}
