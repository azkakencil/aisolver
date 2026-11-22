package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.model.AccentTheme
import com.example.model.AppSettings
import com.example.model.ButtonSizeOption
import com.example.model.CameraResolution
import com.example.model.ChatWindowSize
import com.example.model.FloatingPosition
import com.example.model.ScanQuality
import com.example.model.ThemeMode
import com.example.ui.components.GlassCard
import com.example.ui.theme.AiSolverTheme
import com.example.ui.theme.LocalUiDimensions
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  settings: AppSettings,
  hasOverlayPermission: Boolean,
  isSystemOverlayRunning: Boolean,
  onToggleSystemOverlay: (Boolean) -> Unit,
  onOpenOverlayPermission: () -> Unit,
  onUpdateSetting: ((AppSettings) -> AppSettings) -> Unit,
  onEnterPreview: () -> Unit,
  onDeleteAllHistory: () -> Unit,
  onResetSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val dims = LocalUiDimensions.current
  var showClearHistoryDialog by remember { mutableStateOf(false) }
  var showResetDialog by remember { mutableStateOf(false) }
  var showAboutDialog by remember { mutableStateOf(false) }
  var showPrivacyDialog by remember { mutableStateOf(false) }

  if (showClearHistoryDialog) {
    AlertDialog(
      onDismissRequest = { showClearHistoryDialog = false },
      title = { Text("Hapus Semua Riwayat?") },
      text = { Text("Seluruh riwayat percakapan dan hasil scan soal akan dihapus permanen.") },
      confirmButton = {
        Button(
          onClick = {
            showClearHistoryDialog = false
            onDeleteAllHistory()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) {
          Text("Hapus Semua")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearHistoryDialog = false }) { Text("Batal") }
      },
    )
  }

  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      title = { Text("Reset Pengaturan?") },
      text = { Text("Semua pengaturan tampilan, chatbot, dan scan akan dikembalikan ke default.") },
      confirmButton = {
        Button(
          onClick = {
            showResetDialog = false
            onResetSettings()
          }
        ) {
          Text("Reset")
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
      },
    )
  }

  if (showAboutDialog) {
    AlertDialog(
      onDismissRequest = { showAboutDialog = false },
      title = { Text("Tentang AI Solver") },
      text = {
        Text(
          "AI Solver – Smart Homework Assistant (v${BuildConfig.VERSION_NAME})\n\n" +
            "Asisten belajar pintar dengan Floating AI Chatbot, CameraX + ML Kit OCR Scanner, " +
            "dan integrasi API Claude AI untuk membantu memahami langkah penyelesaian soal."
        )
      },
      confirmButton = {
        TextButton(onClick = { showAboutDialog = false }) { Text("Tutup") }
      },
    )
  }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Kebijakan Privasi") },
      text = {
        Text(
          "1. Foto soal diproses secara lokal di perangkat menggunakan Google ML Kit OCR.\n" +
            "2. Hanya teks pertanyaan yang dikirim ke endpoint AI untuk memperoleh jawaban.\n" +
            "3. Riwayat percakapan dan pengaturan tersimpan secara lokal di perangkat Anda melalui Room & DataStore.\n" +
            "4. Anda dapat menghapus semua riwayat kapan saja di halaman Pengaturan."
        )
      },
      confirmButton = {
        TextButton(onClick = { showPrivacyDialog = false }) { Text("Mengerti") }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize().testTag("settings_screen"),
    contentPadding = PaddingValues(horizontal = dims.spacingMedium, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(dims.spacingMedium),
  ) {
    // IMPORTANT: Prominent PREVIEW button at the very top of Settings!
    item {
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier =
            Modifier.background(
                Brush.linearGradient(
                  listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                  )
                )
              )
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Pengaturan Aplikasi",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
              )
              Text(
                text = "Uji coba tema, font, tombol, & floating chatbot sebelum menerapkan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Button(
            onClick = onEnterPreview,
            shape = RoundedCornerShape(14.dp),
            colors =
              ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(min = dims.buttonHeight)
                .testTag("settings_preview_button"),
          ) {
            Icon(Icons.Filled.Visibility, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("PREVIEW TAMPILAN", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Section 1: Tampilan
    item {
      SettingsSectionCard(title = "Tampilan", icon = Icons.Filled.Palette) {
        // Theme Mode (Dark / Light / System Default)
        SettingsLabel("Mode Tema")
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ThemeMode.entries.forEach { mode ->
            FilterChip(
              selected = settings.themeMode == mode,
              onClick = { onUpdateSetting { it.copy(themeMode = mode) } },
              label = { Text(mode.label) },
              modifier = Modifier.testTag("setting_theme_${mode.name.lowercase()}"),
            )
          }
        }

        HorizontalDivider(color = dims.subtleBorder.copy(alpha = 0.5f))

        // Warna Tema Aplikasi
        SettingsLabel("Warna Tema Aplikasi")
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AccentTheme.entries.forEach { accent ->
            FilterChip(
              selected = settings.accentTheme == accent,
              onClick = { onUpdateSetting { it.copy(accentTheme = accent) } },
              label = { Text(accent.label) },
              colors =
                FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
              modifier = Modifier.testTag("setting_accent_${accent.name.lowercase()}"),
            )
          }
        }

        HorizontalDivider(color = dims.subtleBorder.copy(alpha = 0.5f))

        // Ukuran Font
        SettingsSliderRow(
          title = "Ukuran Font",
          valueText = "${(settings.fontScale * 100).roundToInt()}%",
          value = settings.fontScale,
          range = 0.85f..1.30f,
          onValueChange = { v -> onUpdateSetting { it.copy(fontScale = v) } },
        )

        // Ukuran Tombol
        SettingsLabel("Ukuran Tombol")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ButtonSizeOption.entries.forEach { sizeOpt ->
            FilterChip(
              selected = settings.buttonSize == sizeOpt,
              onClick = { onUpdateSetting { it.copy(buttonSize = sizeOpt) } },
              label = { Text(sizeOpt.label) },
              modifier = Modifier.weight(1f).testTag("setting_btn_size_${sizeOpt.name.lowercase()}"),
            )
          }
        }

        // Skala Tampilan Aplikasi
        SettingsSliderRow(
          title = "Skala Tampilan Aplikasi",
          valueText = "${(settings.uiScale * 100).roundToInt()}%",
          value = settings.uiScale,
          range = 0.88f..1.15f,
          onValueChange = { v -> onUpdateSetting { it.copy(uiScale = v) } },
        )

        // Transparansi Floating Chatbot
        SettingsSliderRow(
          title = "Transparansi Floating Chatbot",
          valueText = "${(settings.floatingOpacity * 100).roundToInt()}%",
          value = settings.floatingOpacity,
          range = 0.55f..1.0f,
          onValueChange = { v -> onUpdateSetting { it.copy(floatingOpacity = v) } },
        )

        // Posisi Tombol Floating
        SettingsLabel("Posisi Tombol Floating")
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FloatingPosition.entries.forEach { pos ->
            FilterChip(
              selected = settings.floatingPosition == pos,
              onClick = { onUpdateSetting { it.copy(floatingPosition = pos) } },
              label = { Text(pos.label) },
              modifier = Modifier.testTag("setting_fab_pos_${pos.name.lowercase()}"),
            )
          }
        }
      }
    }

    // Section 2: Pengaturan Chatbot & Layar Mengambang
    item {
      SettingsSectionCard(title = "Pengaturan Chatbot", icon = Icons.Filled.SmartToy) {
        SettingsSwitchRow(
          title = "Layar Mengambang (Di Atas Aplikasi Lain)",
          subtitle =
            if (hasOverlayPermission) {
              "Izin aktif • Tampilkan gelembung AI di atas aplikasi lain"
            } else {
              "Butuh izin 'Tampilkan di atas aplikasi lain' dari sistem"
            },
          checked = isSystemOverlayRunning,
          onCheckedChange = { v -> onToggleSystemOverlay(v) },
          testTag = "setting_system_overlay_switch",
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onToggleSystemOverlay(true) },
            colors =
              ButtonDefaults.buttonColors(
                containerColor =
                  if (isSystemOverlayRunning) MaterialTheme.colorScheme.secondary
                  else MaterialTheme.colorScheme.primary,
              ),
            modifier =
              Modifier.weight(1f)
                .heightIn(min = 48.dp)
                .testTag("setting_overlay_on_btn"),
          ) {
            Text(
              if (isSystemOverlayRunning) "ON (Aktif)" else "ON",
              fontWeight = FontWeight.Bold,
            )
          }
          OutlinedButton(
            onClick = { onToggleSystemOverlay(false) },
            enabled = isSystemOverlayRunning,
            modifier =
              Modifier.weight(1f)
                .heightIn(min = 48.dp)
                .testTag("setting_overlay_off_btn"),
          ) {
            Text(
              if (!isSystemOverlayRunning) "OFF (Mati)" else "OFF",
              fontWeight = FontWeight.Bold,
            )
          }
        }

        OutlinedButton(
          onClick = onOpenOverlayPermission,
          modifier =
            Modifier.fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("setting_overlay_permission_btn"),
        ) {
          Text(
            if (hasOverlayPermission) "Kelola Izin Tampilkan di Atas Aplikasi Lain (Aktif)"
            else "Buka Izin Tampilkan di Atas Aplikasi Lain"
          )
        }

        HorizontalDivider(color = dims.subtleBorder.copy(alpha = 0.5f))

        SettingsSwitchRow(
          title = "Aktifkan Floating Chatbot Dalam Aplikasi",
          subtitle = "Tampilkan tombol chatbot mengambang di seluruh halaman aplikasi",
          checked = settings.floatingChatEnabled,
          onCheckedChange = { v -> onUpdateSetting { it.copy(floatingChatEnabled = v) } },
          testTag = "setting_floating_enabled_switch",
        )

        SettingsLabel("Ukuran Jendela Chatbot")
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ChatWindowSize.entries.forEach { wSize ->
            FilterChip(
              selected = settings.chatWindowSize == wSize,
              onClick = { onUpdateSetting { it.copy(chatWindowSize = wSize) } },
              label = { Text(wSize.label) },
              modifier = Modifier.weight(1f),
            )
          }
        }

        SettingsSwitchRow(
          title = "Animasi Chatbot & Streaming",
          subtitle = "Animasi transisi panel dan efek pengetikan respons AI",
          checked = settings.chatAnimationEnabled,
          onCheckedChange = { v -> onUpdateSetting { it.copy(chatAnimationEnabled = v) } },
          testTag = "setting_chat_animation_switch",
        )

        SettingsSwitchRow(
          title = "Tampilkan Riwayat Chat di Beranda",
          subtitle = "Munculkan daftar pertanyaan & jawaban terdahulu",
          checked = settings.showChatHistory,
          onCheckedChange = { v -> onUpdateSetting { it.copy(showChatHistory = v) } },
          testTag = "setting_show_history_switch",
        )

        OutlinedButton(
          onClick = { showClearHistoryDialog = true },
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setting_clear_history_btn"),
        ) {
          Icon(Icons.Filled.DeleteForever, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Hapus Semua Riwayat Chat")
        }
      }
    }

    // Section 3: Pengaturan Scan
    item {
      SettingsSectionCard(title = "Pengaturan Scan", icon = Icons.Filled.CameraAlt) {
        SettingsLabel("Kualitas Gambar")
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ScanQuality.entries.forEach { q ->
            FilterChip(
              selected = settings.imageQuality == q,
              onClick = { onUpdateSetting { it.copy(imageQuality = q) } },
              label = { Text(q.label) },
            )
          }
        }

        SettingsLabel("Resolusi Kamera")
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          CameraResolution.entries.forEach { res ->
            FilterChip(
              selected = settings.cameraResolution == res,
              onClick = { onUpdateSetting { it.copy(cameraResolution = res) } },
              label = { Text(res.label) },
            )
          }
        }

        SettingsSwitchRow(
          title = "OCR Otomatis",
          subtitle = "Langsung membaca teks dan menyelesaikan soal setelah foto diambil",
          checked = settings.autoOcr,
          onCheckedChange = { v -> onUpdateSetting { it.copy(autoOcr = v) } },
          testTag = "setting_auto_ocr_switch",
        )

        SettingsSwitchRow(
          title = "Crop Gambar Otomatis",
          subtitle = "Deteksi area teks soal otomatis tanpa dialog potong manual",
          checked = settings.autoCrop,
          onCheckedChange = { v -> onUpdateSetting { it.copy(autoCrop = v) } },
          testTag = "setting_auto_crop_switch",
        )
      }
    }

    // Section 4: Pengaturan Aplikasi
    item {
      SettingsSectionCard(title = "Pengaturan Aplikasi", icon = Icons.Filled.Info) {
        SettingsInfoRow("Nama Aplikasi", "AI Solver – Smart Homework Assistant")
        SettingsInfoRow("Versi Aplikasi", BuildConfig.VERSION_NAME)

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = { showAboutDialog = true },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) {
            Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Tentang")
          }
          OutlinedButton(
            onClick = { showPrivacyDialog = true },
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
          ) {
            Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Privasi")
          }
        }

        Button(
          onClick = { showResetDialog = true },
          colors =
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
              contentColor = MaterialTheme.colorScheme.onSurface,
            ),
          modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("setting_reset_button"),
        ) {
          Icon(Icons.Filled.RestartAlt, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text("Reset Semua Pengaturan")
        }
      }
    }

    item { Spacer(Modifier.height(84.dp)) }
  }
}

@Composable
fun SettingsPreviewOverlay(
  draftSettings: AppSettings,
  onUpdateDraft: ((AppSettings) -> AppSettings) -> Unit,
  onApply: () -> Unit,
  onCancel: () -> Unit,
) {
  // Wrap Preview inside AiSolverTheme(draftSettings) so users see every theme/font/button change live!
  AiSolverTheme(settings = draftSettings) {
    val dims = LocalUiDimensions.current
    Surface(
      color = MaterialTheme.colorScheme.background,
      modifier = Modifier.fillMaxSize().testTag("settings_preview_overlay"),
    ) {
      Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
      ) {
        // Preview Header
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .background(MaterialTheme.colorScheme.surface)
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text(
              text = "Mode Preview Tampilan",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Coba & lihat perubahan sebelum menyimpan",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = "SIMULASI LIVE",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        // Live Simulated App Canvas + Quick Interactive Controls
        Column(
          modifier =
            Modifier.weight(1f)
              .verticalScroll(rememberScrollState())
              .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          // Simulated Mini Phone Preview Frame
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            shadowElevation = 10.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
              Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Simulated Header
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "AI Solver",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                  )
                  Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                  ) {
                    Text(
                      text = draftSettings.themeMode.label,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                  }
                }

                // Simulated Sample Answer Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                  Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = "Contoh Soal: Berapa hasil 2x + 4 = 12?",
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                      text = "Langkah 1: Kurangi kedua sisi dengan 4 -> 2x = 8.\nLangkah 2: Bagi dengan 2 -> x = 4.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }

                // Simulated Buttons respecting buttonSize & uiScale
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Button(
                    onClick = {},
                    modifier = Modifier.weight(1f).heightIn(min = dims.buttonHeight),
                  ) {
                    Text("Mulai Chat AI")
                  }
                  OutlinedButton(
                    onClick = {},
                    modifier = Modifier.weight(1f).heightIn(min = dims.buttonHeight),
                  ) {
                    Text("Scan Soal")
                  }
                }

                // Simulated Floating Chatbot Panel & FAB respecting opacity & position
                if (draftSettings.floatingChatEnabled) {
                  val fabAlign =
                    when (draftSettings.floatingPosition) {
                      FloatingPosition.BOTTOM_RIGHT,
                      FloatingPosition.MIDDLE_RIGHT -> Alignment.CenterEnd
                      FloatingPosition.BOTTOM_LEFT,
                      FloatingPosition.MIDDLE_LEFT -> Alignment.CenterStart
                    }
                  Surface(
                    shape = RoundedCornerShape(18.dp),
                    color =
                      dims.cardSurface.copy(
                        alpha = draftSettings.floatingOpacity.coerceIn(0.55f, 1.0f)
                      ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth().alpha(draftSettings.floatingOpacity),
                  ) {
                    Column(
                      modifier = Modifier.padding(12.dp),
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                      ) {
                        Icon(
                          Icons.Filled.AutoAwesome,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(16.dp),
                        )
                        Text(
                          text =
                            "Preview Floating Chatbot (${draftSettings.chatWindowSize.label})",
                          style = MaterialTheme.typography.labelLarge,
                          fontWeight = FontWeight.Bold,
                        )
                      }
                      Text(
                        text =
                          "Posisi FAB: ${draftSettings.floatingPosition.label} • Opasitas: ${(draftSettings.floatingOpacity * 100).roundToInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                          contentAlignment = Alignment.Center,
                          modifier =
                            Modifier.align(fabAlign)
                              .size(44.dp)
                              .clip(CircleShape)
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
                            Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                          )
                        }
                      }
                    }
                  }
                } else {
                  Text(
                    text = "Floating Chatbot saat ini dinonaktifkan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }

          // Quick Live Tweakers inside Preview
          GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Text(
                text = "Ubah Cepat di Mode Preview",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ThemeMode.entries.forEach { mode ->
                  FilterChip(
                    selected = draftSettings.themeMode == mode,
                    onClick = { onUpdateDraft { it.copy(themeMode = mode) } },
                    label = { Text(mode.label) },
                  )
                }
              }
              Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                AccentTheme.entries.forEach { acc ->
                  FilterChip(
                    selected = draftSettings.accentTheme == acc,
                    onClick = { onUpdateDraft { it.copy(accentTheme = acc) } },
                    label = { Text(acc.label) },
                  )
                }
              }
              SettingsSliderRow(
                title = "Ukuran Font",
                valueText = "${(draftSettings.fontScale * 100).roundToInt()}%",
                value = draftSettings.fontScale,
                range = 0.85f..1.30f,
                onValueChange = { v -> onUpdateDraft { it.copy(fontScale = v) } },
              )
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                ButtonSizeOption.entries.forEach { sz ->
                  FilterChip(
                    selected = draftSettings.buttonSize == sz,
                    onClick = { onUpdateDraft { it.copy(buttonSize = sz) } },
                    label = { Text("Tombol ${sz.label}") },
                    modifier = Modifier.weight(1f),
                  )
                }
              }
              SettingsSliderRow(
                title = "Transparansi Floating Chatbot",
                valueText = "${(draftSettings.floatingOpacity * 100).roundToInt()}%",
                value = draftSettings.floatingOpacity,
                range = 0.55f..1.0f,
                onValueChange = { v -> onUpdateDraft { it.copy(floatingOpacity = v) } },
              )
            }
          }
        }

        // Bottom APPLY and CANCEL bar
        Surface(
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, dims.subtleBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            OutlinedButton(
              onClick = onCancel,
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = 50.dp)
                  .testTag("preview_cancel_button"),
            ) {
              Icon(Icons.Filled.Close, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("CANCEL", fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = onApply,
              shape = RoundedCornerShape(14.dp),
              colors =
                ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondary,
                  contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
              modifier =
                Modifier.weight(1f)
                  .heightIn(min = 50.dp)
                  .testTag("preview_apply_button"),
            ) {
              Icon(Icons.Filled.Check, contentDescription = null)
              Spacer(Modifier.width(6.dp))
              Text("APPLY", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionCard(
  title: String,
  icon: ImageVector,
  content: @Composable () -> Unit,
) {
  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      }
      content()
    }
  }
}

@Composable
private fun SettingsLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.SemiBold,
  )
}

@Composable
private fun SettingsSliderRow(
  title: String,
  valueText: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
      Text(
        valueText,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
      )
    }
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = range,
    )
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
      Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = Modifier.testTag(testTag),
    )
  }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      value,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.SemiBold,
    )
  }
}
