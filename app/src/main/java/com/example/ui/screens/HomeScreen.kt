package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ApiConnectionStatus
import com.example.model.AppSettings
import com.example.model.ChatSessionSummary
import com.example.ui.components.ApiStatusBadge
import com.example.ui.components.GlassCard
import com.example.ui.theme.LocalUiDimensions
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
  settings: AppSettings,
  apiStatus: ApiConnectionStatus,
  hasOverlayPermission: Boolean,
  isSystemOverlayRunning: Boolean,
  sessions: List<ChatSessionSummary>,
  onRefreshApiStatus: () -> Unit,
  onToggleSystemOverlay: (Boolean) -> Unit,
  onOpenOverlayPermission: () -> Unit,
  onStartChat: () -> Unit,
  onOpenScanner: () -> Unit,
  onUploadImage: (Uri) -> Unit,
  onOpenSession: (Long) -> Unit,
  onDeleteSession: (Long) -> Unit,
  onQuickPrompt: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val dims = LocalUiDimensions.current
  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) onUploadImage(uri)
    }

  val quickTopics =
    listOf(
      "Matematika" to
        "Bantu saya memahami cara menyelesaikan persamaan kuadrat 2x² - 5x + 2 = 0 langkah demi langkah.",
      "Fisika" to "Jelaskan hukum Newton II beserta contoh soal dan pembahasannya.",
      "Kimia" to "Jelaskan cara menyetarakan reaksi kimia dengan mudah dan contohnya.",
      "Informatika" to
        "Apa itu algoritma rekursif dan kapan sebaiknya digunakan? Beri contoh kode.",
      "Bahasa" to "Berikan tips menyusun paragraf argumentasi yang kuat dalam bahasa Indonesia.",
      "Sejarah" to "Ringkas latar belakang peristiwa Sumpah Pemuda 1928 beserta maknanya.",
    )

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("home_screen"),
    contentPadding = PaddingValues(horizontal = dims.spacingMedium, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(dims.spacingMedium),
  ) {
    // 1. Header with App Logo, Title & API Connection Status
    item {
      Row(
        modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Surface(
            shape = RoundedCornerShape(15.dp),
            shadowElevation = if (dims.isDark) 0.dp else 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
          ) {
            Image(
              painter = painterResource(id = R.drawable.ic_launcher_art),
              contentDescription = "Logo AI Solver",
              contentScale = ContentScale.Crop,
              modifier = Modifier.size(48.dp),
            )
          }
          Column {
            Text(
              text = "AI Solver",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Smart Homework Assistant",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        ApiStatusBadge(status = apiStatus, onClick = onRefreshApiStatus)
      }
    }

    // 2. Hero Modern AI Dashboard Banner (Vibrant luminous gradient for Light & Dark modes)
    item {
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .shadow(
              elevation = if (dims.isDark) 4.dp else 12.dp,
              shape = RoundedCornerShape(dims.cardCornerRadius + 4.dp),
              ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
              spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(dims.cardCornerRadius + 4.dp))
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.primary,
                  Color(0xFF4932D6),
                  MaterialTheme.colorScheme.secondary,
                )
              )
            ),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_hero_banner),
          contentDescription = "Latar dashboard AI Solver",
          contentScale = ContentScale.Crop,
          modifier = Modifier.matchParentSize().alpha(0.22f),
        )
        Column(
          modifier = Modifier.fillMaxWidth().padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
              )
              Text(
                text = "Claude AI + ML Kit OCR",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          Text(
            text = "Selesaikan Soal Lebih Cepat & Paham Langkahnya",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text =
              "Tanya langsung lewat Chat AI, aktifkan layar mengambang di atas aplikasi lain, atau scan soal kamera.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = onStartChat,
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = Color.White,
                  contentColor = MaterialTheme.colorScheme.primary,
                ),
              contentPadding =
                PaddingValues(horizontal = dims.buttonHorizontalPadding, vertical = 10.dp),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = dims.buttonHeight)
                  .testTag("home_start_chat_button"),
            ) {
              Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                "Mulai Chat AI",
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            OutlinedButton(
              onClick = onOpenScanner,
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
              colors =
                ButtonDefaults.outlinedButtonColors(
                  contentColor = Color.White,
                  containerColor = Color.White.copy(alpha = 0.14f),
                ),
              contentPadding =
                PaddingValues(horizontal = dims.buttonHorizontalPadding, vertical = 10.dp),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = dims.buttonHeight)
                  .testTag("home_scan_button"),
            ) {
              Icon(
                Icons.Filled.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                "Scan Soal",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }

    // 3. Layar Mengambang (Tampilkan di Atas Aplikasi Lain) Card
    item {
      val badgeColor =
        when {
          isSystemOverlayRunning -> SuccessMint
          hasOverlayPermission -> MaterialTheme.colorScheme.onSurfaceVariant
          else -> WarningAmber
        }
      val badgeText =
        when {
          isSystemOverlayRunning -> "ON • Aktif"
          hasOverlayPermission -> "OFF • Siap"
          else -> "Butuh Izin"
        }

      GlassCard(
        modifier = Modifier.fillMaxWidth().testTag("home_system_overlay_card"),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier =
                  Modifier.size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                      if (isSystemOverlayRunning) SuccessMint.copy(alpha = 0.16f)
                      else MaterialTheme.colorScheme.primaryContainer
                    ),
              ) {
                Icon(
                  imageVector = Icons.Filled.Layers,
                  contentDescription = null,
                  tint =
                    if (isSystemOverlayRunning) SuccessMint
                    else MaterialTheme.colorScheme.primary,
                )
              }
              Column {
                Text(
                  text = "Layar Mengambang",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                )
                Text(
                  text = "Tampilkan di atas aplikasi lain",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Surface(
                shape = CircleShape,
                color = badgeColor.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f)),
              ) {
                Text(
                  text = badgeText,
                  style = MaterialTheme.typography.labelSmall,
                  color = badgeColor,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
              }
              Switch(
                checked = isSystemOverlayRunning,
                onCheckedChange = { checked -> onToggleSystemOverlay(checked) },
                modifier = Modifier.testTag("home_system_overlay_switch"),
              )
            }
          }

          Text(
            text =
              if (hasOverlayPermission) {
                "Tekan tombol ON untuk memunculkan gelembung AI mengambang di atas aplikasi lain, atau OFF untuk mematikannya."
              } else {
                "Berikan izin 'Tampilkan di atas aplikasi lain' agar gelembung AI Solver dapat muncul mengambang di luar aplikasi ini."
              },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          // Explicit ON / OFF Buttons for Layar Mengambang
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Button(
              onClick = {
                if (hasOverlayPermission) onToggleSystemOverlay(true)
                else onOpenOverlayPermission()
              },
              shape = RoundedCornerShape(12.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor =
                    if (isSystemOverlayRunning) SuccessMint
                    else MaterialTheme.colorScheme.primary,
                  contentColor = Color.White,
                ),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = dims.buttonHeight)
                  .testTag("home_overlay_on_button"),
            ) {
              Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text =
                  when {
                    isSystemOverlayRunning -> "ON (Aktif)"
                    hasOverlayPermission -> "ON"
                    else -> "ON (Izinkan)"
                  },
                fontWeight = FontWeight.Bold,
              )
            }

            OutlinedButton(
              onClick = { onToggleSystemOverlay(false) },
              enabled = isSystemOverlayRunning,
              shape = RoundedCornerShape(12.dp),
              border =
                BorderStroke(
                  1.5.dp,
                  if (!isSystemOverlayRunning) MaterialTheme.colorScheme.outline
                  else MaterialTheme.colorScheme.error,
                ),
              colors =
                ButtonDefaults.outlinedButtonColors(
                  contentColor =
                    if (!isSystemOverlayRunning) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                  containerColor =
                    if (!isSystemOverlayRunning) MaterialTheme.colorScheme.surfaceContainerHigh
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                ),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = dims.buttonHeight)
                  .testTag("home_overlay_off_button"),
            ) {
              Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(Modifier.width(6.dp))
              Text(
                text = if (!isSystemOverlayRunning) "OFF (Mati)" else "OFF",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // 4. Quick Action Cards (Scan Kamera & Upload Gambar Soal)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        DashboardQuickTile(
          title = "Scan Kamera",
          subtitle = "Deteksi teks otomatis + langkah",
          icon = Icons.Filled.CameraAlt,
          accent = MaterialTheme.colorScheme.primary,
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          onClick = onOpenScanner,
          testTag = "home_tile_scan",
          modifier = Modifier.weight(1f),
        )
        DashboardQuickTile(
          title = "Upload Gambar",
          subtitle = "Pilih foto soal dari galeri",
          icon = Icons.Filled.Image,
          accent = MaterialTheme.colorScheme.secondary,
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          testTag = "home_tile_upload",
          modifier = Modifier.weight(1f),
        )
      }
    }

    // 5. Subject Starter Prompts
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            Icons.Filled.Science,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = "Topik Cepat Mata Pelajaran",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
          )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          items(quickTopics) { (subject, prompt) ->
            GlassCard(
              onClick = { onQuickPrompt(prompt) },
              modifier = Modifier.width(196.dp).testTag("quick_topic_$subject"),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                  Text(
                    text = subject,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                  )
                }
                Text(
                  text = prompt,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    // 6. Riwayat Pertanyaan & Jawaban
    if (settings.showChatHistory) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              Icons.Filled.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "Riwayat Pertanyaan & Jawaban",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
          ) {
            Text(
              text = "${sessions.size} sesi",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      if (sessions.isEmpty()) {
        item {
          GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                Icons.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                modifier = Modifier.size(38.dp),
              )
              Text(
                text = "Belum ada riwayat soal",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text =
                  "Mulai chat AI, aktifkan layar mengambang, atau scan soal pertamamu. Semua pembahasan otomatis tersimpan di sini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(sessions, key = { it.id }) { session ->
          HistorySessionCard(
            session = session,
            onClick = { onOpenSession(session.id) },
            onDelete = { onDeleteSession(session.id) },
          )
        }
      }
    }

    item { Spacer(Modifier.height(84.dp)) }
  }
}

@Composable
private fun DashboardQuickTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  accent: Color,
  containerColor: Color,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  GlassCard(onClick = onClick, modifier = modifier.testTag(testTag)) {
    Column(
      modifier = Modifier.padding(15.dp),
      verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(44.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(containerColor),
      ) {
        Icon(imageVector = icon, contentDescription = title, tint = accent)
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun HistorySessionCard(
  session: ChatSessionSummary,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  val dateStr =
    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(session.updatedAt))

  GlassCard(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().testTag("history_item_${session.id}"),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = session.source.label,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            )
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = session.subjectTag,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            )
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = dateStr,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp).testTag("delete_history_${session.id}"),
          ) {
            Icon(
              Icons.Filled.DeleteOutline,
              contentDescription = "Hapus riwayat",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      Text(
        text = session.lastQuestion,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      if (session.lastAnswer.isNotBlank()) {
        Text(
          text = session.lastAnswer,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
