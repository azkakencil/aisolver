package com.example.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AccentTheme
import com.example.model.AppSettings
import com.example.model.ButtonSizeOption
import com.example.model.CameraResolution
import com.example.model.ChatWindowSize
import com.example.model.FloatingPosition
import com.example.model.ScanQuality
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "ai_solver_settings")

class SettingsDataStore(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT_THEME = stringPreferencesKey("accent_theme")
    val FONT_SCALE = floatPreferencesKey("font_scale")
    val BUTTON_SIZE = stringPreferencesKey("button_size")
    val UI_SCALE = floatPreferencesKey("ui_scale")
    val FLOATING_OPACITY = floatPreferencesKey("floating_opacity")
    val FLOATING_POSITION = stringPreferencesKey("floating_position")
    val FLOATING_CHAT_ENABLED = booleanPreferencesKey("floating_chat_enabled")
    val SYSTEM_OVERLAY_ENABLED = booleanPreferencesKey("system_overlay_enabled")
    val CHAT_WINDOW_SIZE = stringPreferencesKey("chat_window_size")
    val CHAT_ANIMATION_ENABLED = booleanPreferencesKey("chat_animation_enabled")
    val SHOW_CHAT_HISTORY = booleanPreferencesKey("show_chat_history")
    val IMAGE_QUALITY = stringPreferencesKey("image_quality")
    val CAMERA_RESOLUTION = stringPreferencesKey("camera_resolution")
    val AUTO_OCR = booleanPreferencesKey("auto_ocr")
    val AUTO_CROP = booleanPreferencesKey("auto_crop")
  }

  val settingsFlow: Flow<AppSettings> =
    context.settingsDataStore.data.map { prefs ->
      AppSettings(
        themeMode =
          prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.LIGHT,
        accentTheme =
          prefs[Keys.ACCENT_THEME]?.let { runCatching { AccentTheme.valueOf(it) }.getOrNull() }
            ?: AccentTheme.VIOLET_MINT,
        fontScale = (prefs[Keys.FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.30f),
        buttonSize =
          prefs[Keys.BUTTON_SIZE]?.let { runCatching { ButtonSizeOption.valueOf(it) }.getOrNull() }
            ?: ButtonSizeOption.MEDIUM,
        uiScale = (prefs[Keys.UI_SCALE] ?: 1.0f).coerceIn(0.88f, 1.15f),
        floatingOpacity = (prefs[Keys.FLOATING_OPACITY] ?: 0.96f).coerceIn(0.55f, 1.0f),
        floatingPosition =
          prefs[Keys.FLOATING_POSITION]?.let {
            runCatching { FloatingPosition.valueOf(it) }.getOrNull()
          } ?: FloatingPosition.BOTTOM_RIGHT,
        floatingChatEnabled = prefs[Keys.FLOATING_CHAT_ENABLED] ?: true,
        systemOverlayEnabled = prefs[Keys.SYSTEM_OVERLAY_ENABLED] ?: false,
        chatWindowSize =
          prefs[Keys.CHAT_WINDOW_SIZE]?.let {
            runCatching { ChatWindowSize.valueOf(it) }.getOrNull()
          } ?: ChatWindowSize.STANDARD,
        chatAnimationEnabled = prefs[Keys.CHAT_ANIMATION_ENABLED] ?: true,
        showChatHistory = prefs[Keys.SHOW_CHAT_HISTORY] ?: true,
        imageQuality =
          prefs[Keys.IMAGE_QUALITY]?.let { runCatching { ScanQuality.valueOf(it) }.getOrNull() }
            ?: ScanQuality.BALANCED,
        cameraResolution =
          prefs[Keys.CAMERA_RESOLUTION]?.let {
            runCatching { CameraResolution.valueOf(it) }.getOrNull()
          } ?: CameraResolution.FHD_1080P,
        autoOcr = prefs[Keys.AUTO_OCR] ?: true,
        autoCrop = prefs[Keys.AUTO_CROP] ?: true,
      )
    }

  suspend fun saveSettings(settings: AppSettings) {
    context.settingsDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = settings.themeMode.name
      prefs[Keys.ACCENT_THEME] = settings.accentTheme.name
      prefs[Keys.FONT_SCALE] = settings.fontScale
      prefs[Keys.BUTTON_SIZE] = settings.buttonSize.name
      prefs[Keys.UI_SCALE] = settings.uiScale
      prefs[Keys.FLOATING_OPACITY] = settings.floatingOpacity
      prefs[Keys.FLOATING_POSITION] = settings.floatingPosition.name
      prefs[Keys.FLOATING_CHAT_ENABLED] = settings.floatingChatEnabled
      prefs[Keys.SYSTEM_OVERLAY_ENABLED] = settings.systemOverlayEnabled
      prefs[Keys.CHAT_WINDOW_SIZE] = settings.chatWindowSize.name
      prefs[Keys.CHAT_ANIMATION_ENABLED] = settings.chatAnimationEnabled
      prefs[Keys.SHOW_CHAT_HISTORY] = settings.showChatHistory
      prefs[Keys.IMAGE_QUALITY] = settings.imageQuality.name
      prefs[Keys.CAMERA_RESOLUTION] = settings.cameraResolution.name
      prefs[Keys.AUTO_OCR] = settings.autoOcr
      prefs[Keys.AUTO_CROP] = settings.autoCrop
    }
  }

  suspend fun resetToDefaults() {
    saveSettings(AppSettings())
  }
}
