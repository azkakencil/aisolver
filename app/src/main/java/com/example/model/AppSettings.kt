package com.example.model

enum class ThemeMode(val label: String) {
  LIGHT("Light Mode"),
  DARK("Dark Mode"),
  SYSTEM("System Default"),
}

enum class AccentTheme(val label: String) {
  VIOLET_MINT("Violet & Mint"),
  OCEAN_CYAN("Ocean & Cyan"),
  SUNSET_CORAL("Indigo & Coral"),
  EMERALD_GOLD("Emerald & Amber"),
}

enum class ButtonSizeOption(
  val label: String,
  val heightScale: Float,
  val horizontalPaddingDp: Float,
) {
  COMPACT("Kecil", 0.88f, 12f),
  MEDIUM("Sedang", 1.0f, 16f),
  LARGE("Besar", 1.16f, 22f),
}

enum class FloatingPosition(val label: String) {
  BOTTOM_RIGHT("Bawah Kanan"),
  BOTTOM_LEFT("Bawah Kiri"),
  MIDDLE_RIGHT("Tengah Kanan"),
  MIDDLE_LEFT("Tengah Kiri"),
}

enum class ChatWindowSize(val label: String, val heightFraction: Float) {
  COMPACT("Kecil", 0.45f),
  STANDARD("Standar", 0.65f),
  EXPANDED("Besar", 0.88f),
}

enum class ScanQuality(val label: String, val maxDimensionPx: Int, val jpegQuality: Int) {
  FAST("Cepat (Ringan)", 1024, 75),
  BALANCED("Seimbang", 1600, 88),
  HIGH("Tinggi (Detail)", 2400, 95),
}

enum class CameraResolution(val label: String, val width: Int, val height: Int) {
  HD_720P("HD 720p", 1280, 720),
  FHD_1080P("Full HD 1080p", 1920, 1080),
  MAX("Maksimal Perangkat", 3840, 2160),
}

data class AppSettings(
  val themeMode: ThemeMode = ThemeMode.LIGHT,
  val accentTheme: AccentTheme = AccentTheme.VIOLET_MINT,
  val fontScale: Float = 1.0f,
  val buttonSize: ButtonSizeOption = ButtonSizeOption.MEDIUM,
  val uiScale: Float = 1.0f,
  val floatingOpacity: Float = 0.96f,
  val floatingPosition: FloatingPosition = FloatingPosition.BOTTOM_RIGHT,
  val floatingChatEnabled: Boolean = true,
  val systemOverlayEnabled: Boolean = false,
  val chatWindowSize: ChatWindowSize = ChatWindowSize.STANDARD,
  val chatAnimationEnabled: Boolean = true,
  val showChatHistory: Boolean = true,
  val imageQuality: ScanQuality = ScanQuality.BALANCED,
  val cameraResolution: CameraResolution = CameraResolution.FHD_1080P,
  val autoOcr: Boolean = true,
  val autoCrop: Boolean = true,
)

sealed class ApiConnectionStatus {
  data object Checking : ApiConnectionStatus()

  data class Online(val latencyMs: Long) : ApiConnectionStatus()

  data class Offline(val reason: String) : ApiConnectionStatus()
}
