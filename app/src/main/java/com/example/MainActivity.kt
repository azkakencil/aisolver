package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.model.SessionSource
import com.example.ui.components.FloatingChatbotOverlay
import com.example.ui.components.ImageCropperModal
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsPreviewOverlay
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AiSolverTheme
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.flow.MutableStateFlow

const val ROUTE_HOME = "home"
const val ROUTE_SCANNER = "scanner"
const val ROUTE_CHAT = "chat"
const val ROUTE_SETTINGS = "settings"

private data class NavDestinationItem(
  val route: String,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
)

private val navItems =
  listOf(
    NavDestinationItem(ROUTE_HOME, "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
    NavDestinationItem(
      ROUTE_SCANNER,
      "Scan Soal",
      Icons.Filled.DocumentScanner,
      Icons.Outlined.DocumentScanner,
    ),
    NavDestinationItem(
      ROUTE_CHAT,
      "Chat AI",
      Icons.AutoMirrored.Filled.Chat,
      Icons.AutoMirrored.Outlined.Chat,
    ),
    NavDestinationItem(
      ROUTE_SETTINGS,
      "Pengaturan",
      Icons.Filled.Settings,
      Icons.Outlined.Settings,
    ),
  )

class MainActivity : ComponentActivity() {
  companion object {
    const val EXTRA_OPEN_ROUTE = "extra_open_route"
    const val EXTRA_AUTO_PICK_GALLERY = "extra_auto_pick_gallery"
  }

  private val pendingOverlayRoute = MutableStateFlow<String?>(null)
  private val pendingAutoGallery = MutableStateFlow(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    consumeOverlayIntent(intent)
    setContent {
      val requestedRoute by pendingOverlayRoute.collectAsStateWithLifecycle()
      val requestedGallery by pendingAutoGallery.collectAsStateWithLifecycle()
      AiSolverRootApp(
        requestedRoute = requestedRoute,
        requestedAutoGallery = requestedGallery,
        onRequestedRouteHandled = { pendingOverlayRoute.value = null },
        onRequestedGalleryHandled = { pendingAutoGallery.value = false },
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    consumeOverlayIntent(intent)
  }

  private fun consumeOverlayIntent(intent: Intent?) {
    val route = intent?.getStringExtra(EXTRA_OPEN_ROUTE)
    if (!route.isNullOrBlank()) {
      pendingOverlayRoute.value = route
      pendingAutoGallery.value = intent.getBooleanExtra(EXTRA_AUTO_PICK_GALLERY, false)
      intent.removeExtra(EXTRA_OPEN_ROUTE)
      intent.removeExtra(EXTRA_AUTO_PICK_GALLERY)
    }
  }
}

@Composable
fun AiSolverRootApp(
  requestedRoute: String? = null,
  requestedAutoGallery: Boolean = false,
  onRequestedRouteHandled: () -> Unit = {},
  onRequestedGalleryHandled: () -> Unit = {},
  viewModel: MainViewModel = viewModel(),
) {
  val context = LocalContext.current
  val activeSettings by viewModel.activeSettings.collectAsStateWithLifecycle()
  val draftSettings by viewModel.draftSettings.collectAsStateWithLifecycle()
  val isPreviewMode by viewModel.isPreviewMode.collectAsStateWithLifecycle()
  val apiStatus by viewModel.apiStatus.collectAsStateWithLifecycle()
  val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()
  val isSystemOverlayRunning by viewModel.isSystemOverlayRunning.collectAsStateWithLifecycle()
  val sessions by viewModel.sessions.collectAsStateWithLifecycle()
  val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
  val isFloatingChatOpen by viewModel.isFloatingChatOpen.collectAsStateWithLifecycle()
  val floatingWindowOverride by viewModel.floatingWindowOverride.collectAsStateWithLifecycle()
  val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
  val streamingAnswer by viewModel.streamingAnswer.collectAsStateWithLifecycle()
  val chatError by viewModel.chatError.collectAsStateWithLifecycle()
  val scanState by viewModel.scanState.collectAsStateWithLifecycle()
  val pendingCropBitmap by viewModel.pendingCropBitmap.collectAsStateWithLifecycle()
  val suggestedCropRect by viewModel.suggestedCropRect.collectAsStateWithLifecycle()
  val croppedPreviewBitmap by viewModel.croppedPreviewBitmap.collectAsStateWithLifecycle()

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.refreshOverlayPermission()
  }

  fun handleToggleSystemOverlay(enable: Boolean) {
    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val notifGranted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (!notifGranted) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    viewModel.toggleSystemOverlay(enable)
  }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: ROUTE_HOME
  var autoFocusChatInput by remember { mutableStateOf(false) }

  fun navigateTo(route: String) {
    navController.navigate(route) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
    }
  }

  LaunchedEffect(requestedRoute) {
    if (!requestedRoute.isNullOrBlank()) {
      navigateTo(requestedRoute)
      onRequestedRouteHandled()
    }
  }

  AiSolverTheme(settings = activeSettings) {
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                MaterialTheme.colorScheme.background,
              )
            )
          )
    ) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
          containerColor = Color.Transparent,
          contentWindowInsets = WindowInsets.safeDrawing,
          bottomBar = {
            if (!useNavRail) {
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp,
                tonalElevation = 2.dp,
              ) {
                NavigationBar(
                  containerColor = Color.Transparent,
                  modifier = Modifier.navigationBarsPadding(),
                ) {
                  navItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                      selected = selected,
                      onClick = {
                        autoFocusChatInput = item.route == ROUTE_CHAT
                        navigateTo(item.route)
                      },
                      icon = {
                        Icon(
                          imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                          contentDescription = item.label,
                        )
                      },
                      label = { Text(item.label) },
                      modifier = Modifier.testTag("nav_${item.route}"),
                    )
                  }
                }
              }
            }
          },
        ) { innerPadding ->
          Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (useNavRail) {
              NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxHeight(),
              ) {
                navItems.forEach { item ->
                  val selected = currentRoute == item.route
                  NavigationRailItem(
                    selected = selected,
                    onClick = {
                      autoFocusChatInput = item.route == ROUTE_CHAT
                      navigateTo(item.route)
                    },
                    icon = {
                      Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                      )
                    },
                    label = { Text(item.label) },
                    modifier = Modifier.testTag("nav_${item.route}"),
                  )
                }
              }
            }

            NavHost(
              navController = navController,
              startDestination = ROUTE_HOME,
              modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
              composable(ROUTE_HOME) {
                HomeScreen(
                  settings = activeSettings,
                  apiStatus = apiStatus,
                  hasOverlayPermission = hasOverlayPermission,
                  isSystemOverlayRunning = isSystemOverlayRunning,
                  sessions = sessions,
                  onRefreshApiStatus = viewModel::refreshApiStatus,
                  onToggleSystemOverlay = ::handleToggleSystemOverlay,
                  onOpenOverlayPermission = viewModel::openOverlayPermissionSettings,
                  onStartChat = {
                    viewModel.startNewChat()
                    viewModel.openFloatingChat()
                  },
                  onOpenScanner = { navigateTo(ROUTE_SCANNER) },
                  onUploadImage = { uri ->
                    navigateTo(ROUTE_SCANNER)
                    viewModel.onImagePickedFromGallery(uri, solveDirectlyInChat = false)
                  },
                  onOpenSession = { sessionId ->
                    viewModel.selectSession(sessionId)
                    autoFocusChatInput = false
                    navigateTo(ROUTE_CHAT)
                  },
                  onDeleteSession = viewModel::deleteSession,
                  onQuickPrompt = { prompt ->
                    viewModel.startNewChat()
                    viewModel.openFloatingChat()
                    viewModel.sendChatMessage(prompt, source = SessionSource.CHAT)
                  },
                )
              }

              composable(ROUTE_SCANNER) {
                ScannerScreen(
                  settings = activeSettings,
                  scanState = scanState,
                  croppedPreviewBitmap = croppedPreviewBitmap,
                  autoLaunchGallery = requestedAutoGallery,
                  onAutoLaunchGalleryHandled = onRequestedGalleryHandled,
                  onImageCaptured = viewModel::onImageCapturedFromCamera,
                  onPickGalleryUri = { uri ->
                    viewModel.onImagePickedFromGallery(uri, solveDirectlyInChat = false)
                  },
                  onOpenCropper = viewModel::openManualCropper,
                  onRunOcrAgain = viewModel::runOcrAgain,
                  onUpdateRecognizedText = viewModel::updateRecognizedText,
                  onSelectSubject = viewModel::updateScanSubject,
                  onSolveQuestion = { source -> viewModel.solveRecognizedQuestion(source) },
                  onTransformSolution = viewModel::transformScanSolution,
                  onClearScan = viewModel::clearScanResult,
                )
              }

              composable(ROUTE_CHAT) {
                ChatScreen(
                  settings = activeSettings,
                  messages = activeMessages,
                  isGenerating = isGenerating,
                  streamingAnswer = streamingAnswer,
                  chatError = chatError,
                  autoFocusInput = autoFocusChatInput,
                  onSendMessage = { text, subject ->
                    viewModel.sendChatMessage(
                      question = text,
                      source = SessionSource.CHAT,
                      subjectTag = subject,
                    )
                  },
                  onNewChat = viewModel::startNewChat,
                  onClearChat = viewModel::clearCurrentConversation,
                  onOpenScanner = { navigateTo(ROUTE_SCANNER) },
                  onUploadImageUri = { uri ->
                    viewModel.onImagePickedFromGallery(uri, solveDirectlyInChat = true)
                  },
                  onRegenerate = { msg -> viewModel.regenerateLastAnswer(msg) },
                  onExplain = viewModel::explainAnswer,
                  onSummarize = viewModel::summarizeAnswer,
                  onTranslate = viewModel::translateAnswer,
                  onDismissError = viewModel::dismissChatError,
                )
              }

              composable(ROUTE_SETTINGS) {
                SettingsScreen(
                  settings = activeSettings,
                  hasOverlayPermission = hasOverlayPermission,
                  isSystemOverlayRunning = isSystemOverlayRunning,
                  onToggleSystemOverlay = ::handleToggleSystemOverlay,
                  onOpenOverlayPermission = viewModel::openOverlayPermissionSettings,
                  onUpdateSetting = viewModel::updateAndSaveSetting,
                  onEnterPreview = viewModel::enterPreviewMode,
                  onDeleteAllHistory = viewModel::deleteAllHistory,
                  onResetSettings = viewModel::resetAllSettings,
                )
              }
            }
          }
        }
      }

      // Always-on-top Floating Chatbot Overlay inside app
      if (!isPreviewMode && pendingCropBitmap == null) {
        if (isFloatingChatOpen) {
          BackHandler { viewModel.closeFloatingChat() }
        }
        FloatingChatbotOverlay(
          settings = activeSettings,
          isOpen = isFloatingChatOpen,
          windowOverride = floatingWindowOverride,
          messages = activeMessages,
          isGenerating = isGenerating,
          streamingAnswer = streamingAnswer,
          chatError = chatError,
          onToggleOpen = viewModel::toggleFloatingChat,
          onClose = viewModel::closeFloatingChat,
          onCycleSize = { viewModel.cycleFloatingWindowSize(activeSettings.chatWindowSize) },
          onSendMessage = { text -> viewModel.sendChatMessage(text, source = SessionSource.CHAT) },
          onClearChat = viewModel::clearCurrentConversation,
          onOpenScanner = { navigateTo(ROUTE_SCANNER) },
          onUploadImageUri = { uri ->
            viewModel.onImagePickedFromGallery(uri, solveDirectlyInChat = true)
          },
          onRegenerate = { msg -> viewModel.regenerateLastAnswer(msg) },
          onExplain = viewModel::explainAnswer,
          onSummarize = viewModel::summarizeAnswer,
          onTranslate = viewModel::translateAnswer,
          onDismissError = viewModel::dismissChatError,
        )
      }

      // Interactive Image Cropper Modal
      pendingCropBitmap?.let { bmp ->
        BackHandler { viewModel.cancelCrop() }
        ImageCropperModal(
          bitmap = bmp,
          initialRect = suggestedCropRect,
          onConfirmCrop = { rect -> viewModel.confirmCrop(rect) },
          onCancel = viewModel::cancelCrop,
        )
      }

      // Settings Preview Mode Overlay (APPLY / CANCEL)
      if (isPreviewMode) {
        BackHandler { viewModel.cancelPreviewChanges() }
        SettingsPreviewOverlay(
          draftSettings = draftSettings,
          onUpdateDraft = viewModel::updatePreviewDraft,
          onApply = viewModel::applyPreviewChanges,
          onCancel = viewModel::cancelPreviewChanges,
        )
      }
    }
  }
}
