package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.db.AppDatabase
import com.example.data.local.prefs.SettingsDataStore
import com.example.data.ocr.OcrProcessor
import com.example.data.remote.NetworkModule
import com.example.data.repository.AiSolverRepository
import com.example.data.repository.AiStreamEvent
import com.example.model.ApiConnectionStatus
import com.example.model.AppSettings
import com.example.model.ChatMessageUi
import com.example.model.ChatSessionSummary
import com.example.model.ChatWindowSize
import com.example.model.ScanState
import com.example.model.SessionSource
import com.example.model.SubjectTag
import com.example.overlay.FloatingOverlayService
import com.example.overlay.OverlayHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val settingsStore = SettingsDataStore(application)
  private val database = AppDatabase.getInstance(application)
  private val repository =
    AiSolverRepository(application, NetworkModule.aiApiService, database.chatDao())
  private val ocrProcessor = OcrProcessor(application)

  // Persisted settings
  val savedSettings: StateFlow<AppSettings> =
    settingsStore.settingsFlow.stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = AppSettings(),
    )

  // Draft settings used in Settings & Preview Mode
  private val _draftSettings = MutableStateFlow(AppSettings())
  val draftSettings: StateFlow<AppSettings> = _draftSettings.asStateFlow()

  private val _isPreviewMode = MutableStateFlow(false)
  val isPreviewMode: StateFlow<Boolean> = _isPreviewMode.asStateFlow()

  private val _hasUnsavedDraftChanges = MutableStateFlow(false)
  val hasUnsavedDraftChanges: StateFlow<Boolean> = _hasUnsavedDraftChanges.asStateFlow()

  // Active effective settings: if in Preview Mode, render using draftSettings so the user sees live changes
  val activeSettings: StateFlow<AppSettings> =
    kotlinx.coroutines.flow
      .combine(savedSettings, _draftSettings, _isPreviewMode) { saved, draft, preview ->
        if (preview) draft else saved
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

  // API Status
  private val _apiStatus = MutableStateFlow<ApiConnectionStatus>(ApiConnectionStatus.Checking)
  val apiStatus: StateFlow<ApiConnectionStatus> = _apiStatus.asStateFlow()

  // System-wide Overlay ("Tampilkan di atas aplikasi lain") state
  private val _hasOverlayPermission =
    MutableStateFlow(OverlayHelper.canDrawOverlays(application))
  val hasOverlayPermission: StateFlow<Boolean> = _hasOverlayPermission.asStateFlow()

  val isSystemOverlayRunning: StateFlow<Boolean> = FloatingOverlayService.isRunningFlow
  private var pendingOverlayStart = false

  // Chat sessions history
  val sessions: StateFlow<List<ChatSessionSummary>> =
    repository.sessionSummaries.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList(),
    )

  private val _activeSessionId = MutableStateFlow<Long?>(null)
  val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

  val activeMessages: StateFlow<List<ChatMessageUi>> =
    _activeSessionId
      .flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeMessages(id)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Floating chatbot state
  private val _isFloatingChatOpen = MutableStateFlow(false)
  val isFloatingChatOpen: StateFlow<Boolean> = _isFloatingChatOpen.asStateFlow()

  private val _floatingWindowOverride = MutableStateFlow<ChatWindowSize?>(null)
  val floatingWindowOverride: StateFlow<ChatWindowSize?> = _floatingWindowOverride.asStateFlow()

  // Streaming / Thinking state
  private val _isGenerating = MutableStateFlow(false)
  val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

  private val _streamingAnswer = MutableStateFlow("")
  val streamingAnswer: StateFlow<String> = _streamingAnswer.asStateFlow()

  private val _chatError = MutableStateFlow<String?>(null)
  val chatError: StateFlow<String?> = _chatError.asStateFlow()

  // Scanner & Crop state
  private val _scanState = MutableStateFlow(ScanState())
  val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

  private val _pendingCropBitmap = MutableStateFlow<Bitmap?>(null)
  val pendingCropBitmap: StateFlow<Bitmap?> = _pendingCropBitmap.asStateFlow()

  private val _suggestedCropRect = MutableStateFlow(RectF(0.08f, 0.15f, 0.92f, 0.85f))
  val suggestedCropRect: StateFlow<RectF> = _suggestedCropRect.asStateFlow()

  private val _croppedPreviewBitmap = MutableStateFlow<Bitmap?>(null)
  val croppedPreviewBitmap: StateFlow<Bitmap?> = _croppedPreviewBitmap.asStateFlow()

  private var chatJob: Job? = null
  private var scanJob: Job? = null

  init {
    viewModelScope.launch {
      savedSettings.collect { latest ->
        if (!_hasUnsavedDraftChanges.value && !_isPreviewMode.value) {
          _draftSettings.value = latest
        }
      }
    }
    refreshApiStatus()
  }

  fun refreshApiStatus() {
    viewModelScope.launch {
      _apiStatus.value = ApiConnectionStatus.Checking
      _apiStatus.value = repository.checkApiStatus()
    }
  }

  fun refreshOverlayPermission() {
    val app = getApplication<Application>()
    val granted = OverlayHelper.canDrawOverlays(app)
    _hasOverlayPermission.value = granted
    if (granted && pendingOverlayStart) {
      pendingOverlayStart = false
      updateAndSaveSetting { it.copy(systemOverlayEnabled = true) }
      OverlayHelper.startOverlayService(app)
    }
  }

  fun openOverlayPermissionSettings() {
    pendingOverlayStart = true
    OverlayHelper.openOverlayPermissionSettings(getApplication())
  }

  fun toggleSystemOverlay(enable: Boolean) {
    val app = getApplication<Application>()
    if (enable) {
      if (OverlayHelper.canDrawOverlays(app)) {
        _hasOverlayPermission.value = true
        updateAndSaveSetting { it.copy(systemOverlayEnabled = true) }
        OverlayHelper.startOverlayService(app)
      } else {
        pendingOverlayStart = true
        OverlayHelper.openOverlayPermissionSettings(app)
      }
    } else {
      pendingOverlayStart = false
      updateAndSaveSetting { it.copy(systemOverlayEnabled = false) }
      OverlayHelper.stopOverlayService(app)
    }
  }

  // --- Floating Chatbot Controls ---

  fun openFloatingChat() {
    _isFloatingChatOpen.value = true
  }

  fun closeFloatingChat() {
    _isFloatingChatOpen.value = false
  }

  fun toggleFloatingChat() {
    _isFloatingChatOpen.value = !_isFloatingChatOpen.value
  }

  fun cycleFloatingWindowSize(defaultSize: ChatWindowSize) {
    val current = _floatingWindowOverride.value ?: defaultSize
    _floatingWindowOverride.value =
      when (current) {
        ChatWindowSize.COMPACT -> ChatWindowSize.STANDARD
        ChatWindowSize.STANDARD -> ChatWindowSize.EXPANDED
        ChatWindowSize.EXPANDED -> ChatWindowSize.COMPACT
      }
  }

  fun setFloatingWindowSize(size: ChatWindowSize) {
    _floatingWindowOverride.value = size
  }

  fun startNewChat() {
    chatJob?.cancel()
    _isGenerating.value = false
    _streamingAnswer.value = ""
    _chatError.value = null
    _activeSessionId.value = null
  }

  fun selectSession(sessionId: Long) {
    _chatError.value = null
    _activeSessionId.value = sessionId
  }

  fun clearCurrentConversation() {
    val currentId = _activeSessionId.value
    chatJob?.cancel()
    _isGenerating.value = false
    _streamingAnswer.value = ""
    _chatError.value = null
    if (currentId != null) {
      viewModelScope.launch {
        repository.clearSessionMessages(currentId)
        _activeSessionId.value = null
      }
    }
  }

  fun deleteSession(sessionId: Long) {
    viewModelScope.launch {
      repository.deleteSession(sessionId)
      if (_activeSessionId.value == sessionId) {
        _activeSessionId.value = null
      }
    }
  }

  fun deleteAllHistory() {
    viewModelScope.launch {
      repository.deleteAllHistory()
      _activeSessionId.value = null
    }
  }

  fun dismissChatError() {
    _chatError.value = null
  }

  // --- Chat Messaging & AI Actions ---

  fun sendChatMessage(
    question: String,
    source: SessionSource = SessionSource.CHAT,
    subjectTag: SubjectTag = SubjectTag.ALL,
    ocrText: String? = null,
  ) {
    val trimmed = question.trim()
    if (trimmed.isEmpty() || _isGenerating.value) return

    chatJob?.cancel()
    chatJob =
      viewModelScope.launch {
        _chatError.value = null
        _isGenerating.value = true
        _streamingAnswer.value = ""

        val sessionId =
          _activeSessionId.value
            ?: repository.createSession(trimmed, source, subjectTag).also {
              _activeSessionId.value = it
            }

        repository.saveUserMessage(
          sessionId = sessionId,
          content = trimmed,
          ocrText = ocrText,
          subjectTag = subjectTag.label,
        )

        val apiPrompt =
          if (ocrText != null) {
            repository.buildHomeworkPrompt(ocrText, subjectTag)
          } else {
            trimmed
          }

        executeAiStream(sessionId, apiPrompt)
      }
  }

  fun regenerateLastAnswer(message: ChatMessageUi? = null) {
    val msgs = activeMessages.value
    val targetSession = message?.sessionId ?: _activeSessionId.value ?: return
    val userPrompt =
      if (message != null) {
        msgs.lastOrNull { it.timestamp <= message.timestamp && it.role == com.example.model.MessageRole.USER }?.content
      } else {
        msgs.lastOrNull { it.role == com.example.model.MessageRole.USER }?.content
      } ?: return

    if (_isGenerating.value) return
    chatJob?.cancel()
    chatJob =
      viewModelScope.launch {
        _chatError.value = null
        _isGenerating.value = true
        _streamingAnswer.value = ""
        executeAiStream(targetSession, userPrompt)
      }
  }

  fun explainAnswer(answerText: String) {
    val sessionId = _activeSessionId.value ?: return
    if (_isGenerating.value) return
    chatJob?.cancel()
    chatJob =
      viewModelScope.launch {
        _chatError.value = null
        _isGenerating.value = true
        _streamingAnswer.value = ""
        repository.saveUserMessage(sessionId, "Jelaskan langkah demi langkah lebih detail")
        executeAiStream(sessionId, repository.buildExplainPrompt(answerText))
      }
  }

  fun summarizeAnswer(answerText: String) {
    val sessionId = _activeSessionId.value ?: return
    if (_isGenerating.value) return
    chatJob?.cancel()
    chatJob =
      viewModelScope.launch {
        _chatError.value = null
        _isGenerating.value = true
        _streamingAnswer.value = ""
        repository.saveUserMessage(sessionId, "Ringkas jawaban di atas")
        executeAiStream(sessionId, repository.buildSummarizePrompt(answerText))
      }
  }

  fun translateAnswer(answerText: String, targetLanguage: String) {
    val sessionId = _activeSessionId.value ?: return
    if (_isGenerating.value) return
    chatJob?.cancel()
    chatJob =
      viewModelScope.launch {
        _chatError.value = null
        _isGenerating.value = true
        _streamingAnswer.value = ""
        repository.saveUserMessage(sessionId, "Ubah bahasa ke $targetLanguage")
        executeAiStream(sessionId, repository.buildTranslatePrompt(answerText, targetLanguage))
      }
  }

  private suspend fun executeAiStream(sessionId: Long, prompt: String) {
    val animate = activeSettings.value.chatAnimationEnabled
    val startTime = System.currentTimeMillis()
    repository.askAiStreaming(prompt, animateStreaming = animate).collect { event ->
      when (event) {
        is AiStreamEvent.Partial -> {
          _streamingAnswer.value = event.text
        }
        is AiStreamEvent.Completed -> {
          _streamingAnswer.value = ""
          _isGenerating.value = false
          repository.saveAssistantMessage(sessionId, event.fullText, isError = false)
          _apiStatus.value =
            ApiConnectionStatus.Online((System.currentTimeMillis() - startTime).coerceAtLeast(1L))
        }
        is AiStreamEvent.Error -> {
          _streamingAnswer.value = ""
          _isGenerating.value = false
          _chatError.value = event.message
        }
      }
    }
  }

  // --- Camera / Gallery / OCR / Homework Solve ---

  fun onImagePickedFromGallery(uri: Uri, solveDirectlyInChat: Boolean = false) {
    viewModelScope.launch {
      _scanState.update { it.copy(isProcessingOcr = true, ocrError = null, solveError = null) }
      try {
        val settings = savedSettings.value
        val bitmap = ocrProcessor.decodeBitmapFromUri(uri, settings.imageQuality)
        handleCapturedBitmap(bitmap, settings, solveDirectlyInChat)
      } catch (e: Exception) {
        _scanState.update {
          it.copy(
            isProcessingOcr = false,
            ocrError = "Gagal memuat gambar: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}",
          )
        }
      }
    }
  }

  fun onImageCapturedFromCamera(bitmap: Bitmap) {
    viewModelScope.launch {
      _scanState.update { it.copy(isProcessingOcr = true, ocrError = null, solveError = null) }
      try {
        val settings = savedSettings.value
        val scaled = ocrProcessor.scaleBitmapIfNeeded(bitmap, settings.imageQuality.maxDimensionPx)
        handleCapturedBitmap(scaled, settings, solveDirectlyInChat = false)
      } catch (e: Exception) {
        _scanState.update {
          it.copy(
            isProcessingOcr = false,
            ocrError = "Gagal memproses foto kamera: ${e.localizedMessage}",
          )
        }
      }
    }
  }

  private suspend fun handleCapturedBitmap(
    bitmap: Bitmap,
    settings: AppSettings,
    solveDirectlyInChat: Boolean,
  ) {
    if (settings.autoCrop) {
      val suggested = runCatching { ocrProcessor.detectSuggestedCropRect(bitmap) }
        .getOrDefault(RectF(0.08f, 0.15f, 0.92f, 0.85f))
      _suggestedCropRect.value = suggested
      val cropped = ocrProcessor.cropBitmapNormalized(bitmap, suggested)
      _croppedPreviewBitmap.value = cropped
      _pendingCropBitmap.value = null
      if (settings.autoOcr || solveDirectlyInChat) {
        runOcrOnBitmap(cropped, autoSolve = true, sendToChat = solveDirectlyInChat)
      } else {
        _scanState.update { it.copy(isProcessingOcr = false) }
      }
    } else {
      // Let user interactively crop the question region
      _suggestedCropRect.value = RectF(0.1f, 0.2f, 0.9f, 0.8f)
      _pendingCropBitmap.value = bitmap
      _scanState.update { it.copy(isProcessingOcr = false) }
    }
  }

  fun openManualCropper() {
    val current = _croppedPreviewBitmap.value ?: return
    _suggestedCropRect.value = RectF(0.06f, 0.08f, 0.94f, 0.92f)
    _pendingCropBitmap.value = current
  }

  fun confirmCrop(normalizedRect: RectF, autoSolveAfterOcr: Boolean = true) {
    val source = _pendingCropBitmap.value ?: return
    viewModelScope.launch {
      _scanState.update { it.copy(isProcessingOcr = true, ocrError = null) }
      try {
        val cropped = ocrProcessor.cropBitmapNormalized(source, normalizedRect)
        _croppedPreviewBitmap.value = cropped
        _pendingCropBitmap.value = null
        runOcrOnBitmap(cropped, autoSolve = autoSolveAfterOcr || savedSettings.value.autoOcr)
      } catch (e: Exception) {
        _pendingCropBitmap.value = null
        _scanState.update {
          it.copy(isProcessingOcr = false, ocrError = "Gagal memotong gambar: ${e.localizedMessage}")
        }
      }
    }
  }

  fun cancelCrop() {
    _pendingCropBitmap.value = null
    _scanState.update { it.copy(isProcessingOcr = false) }
  }

  fun runOcrAgain() {
    val bitmap = _croppedPreviewBitmap.value ?: return
    viewModelScope.launch {
      runOcrOnBitmap(bitmap, autoSolve = false)
    }
  }

  private suspend fun runOcrOnBitmap(
    bitmap: Bitmap,
    autoSolve: Boolean,
    sendToChat: Boolean = false,
  ) {
    _scanState.update { it.copy(isProcessingOcr = true, ocrError = null) }
    try {
      val text = ocrProcessor.recognizeText(bitmap)
      if (text.isBlank()) {
        _scanState.update {
          it.copy(
            isProcessingOcr = false,
            recognizedText = "",
            ocrError =
              "Tidak ada teks yang terdeteksi. Pastikan tulisan soal jelas dan coba potong ulang area soal.",
          )
        }
      } else {
        _scanState.update {
          it.copy(isProcessingOcr = false, recognizedText = text, ocrError = null)
        }
        if (sendToChat) {
          sendChatMessage(
            question = text,
            source = SessionSource.GALLERY_SCAN,
            subjectTag = _scanState.value.selectedSubject,
            ocrText = text,
          )
        } else if (autoSolve) {
          solveRecognizedQuestion()
        }
      }
    } catch (e: Exception) {
      _scanState.update {
        it.copy(
          isProcessingOcr = false,
          ocrError = "OCR gagal membaca gambar: ${e.localizedMessage}",
        )
      }
    }
  }

  fun updateRecognizedText(newText: String) {
    _scanState.update { it.copy(recognizedText = newText, ocrError = null) }
  }

  fun updateScanSubject(subject: SubjectTag) {
    _scanState.update { it.copy(selectedSubject = subject) }
  }

  fun solveRecognizedQuestion(source: SessionSource = SessionSource.CAMERA_SCAN) {
    val questionText = _scanState.value.recognizedText.trim()
    if (questionText.isEmpty() || _scanState.value.isSolving) return

    val subject = _scanState.value.selectedSubject
    scanJob?.cancel()
    scanJob =
      viewModelScope.launch {
        _scanState.update { it.copy(isSolving = true, solveError = null, solutionText = "") }

        val sessionId =
          repository.createSession(
            title = questionText.take(72),
            source = source,
            subjectTag = subject,
          )
        repository.saveUserMessage(
          sessionId = sessionId,
          content = questionText,
          ocrText = questionText,
          subjectTag = subject.label,
        )

        val prompt = repository.buildHomeworkPrompt(questionText, subject)
        val start = System.currentTimeMillis()
        repository.askAiStreaming(prompt, animateStreaming = savedSettings.value.chatAnimationEnabled)
          .collect { event ->
            when (event) {
              is AiStreamEvent.Partial -> {
                _scanState.update { it.copy(solutionText = event.text) }
              }
              is AiStreamEvent.Completed -> {
                _scanState.update {
                  it.copy(
                    isSolving = false,
                    solutionText = event.fullText,
                    solveError = null,
                    activeSessionId = sessionId,
                  )
                }
                repository.saveAssistantMessage(sessionId, event.fullText, isError = false)
                _apiStatus.value =
                  ApiConnectionStatus.Online((System.currentTimeMillis() - start).coerceAtLeast(1L))
              }
              is AiStreamEvent.Error -> {
                _scanState.update {
                  it.copy(
                    isSolving = false,
                    solveError = event.message,
                  )
                }
              }
            }
          }
      }
  }

  fun transformScanSolution(mode: String, targetLang: String = "Inggris") {
    val currentSolution = _scanState.value.solutionText.trim()
    if (currentSolution.isEmpty() || _scanState.value.isSolving) return

    val prompt =
      when (mode) {
        "explain" -> repository.buildExplainPrompt(currentSolution)
        "summarize" -> repository.buildSummarizePrompt(currentSolution)
        "translate" -> repository.buildTranslatePrompt(currentSolution, targetLang)
        else -> return
      }

    scanJob?.cancel()
    scanJob =
      viewModelScope.launch {
        _scanState.update { it.copy(isSolving = true, solveError = null) }
        repository.askAiStreaming(prompt, animateStreaming = savedSettings.value.chatAnimationEnabled)
          .collect { event ->
            when (event) {
              is AiStreamEvent.Partial -> _scanState.update { it.copy(solutionText = event.text) }
              is AiStreamEvent.Completed -> {
                _scanState.update {
                  it.copy(isSolving = false, solutionText = event.fullText, solveError = null)
                }
                _scanState.value.activeSessionId?.let { sid ->
                  repository.saveAssistantMessage(sid, event.fullText, isError = false)
                }
              }
              is AiStreamEvent.Error -> {
                _scanState.update { it.copy(isSolving = false, solveError = event.message) }
              }
            }
          }
      }
  }

  fun clearScanResult() {
    scanJob?.cancel()
    _croppedPreviewBitmap.value = null
    _pendingCropBitmap.value = null
    _scanState.value = ScanState()
  }

  // --- Settings & Preview Mode ---

  fun updateDraftSetting(transform: (AppSettings) -> AppSettings) {
    _hasUnsavedDraftChanges.value = true
    _draftSettings.update(transform)
  }

  /**
   * Updates and immediately saves a setting when user modifies it directly outside preview, OR
   * stages into draft. Here we stage in draft AND persist immediately when not in Preview, OR wait
   * — let's make Settings changes stage into `draftSettings` AND also auto-save OR let user preview?
   * Wait: If the user edits settings and closes the app without clicking Preview->Apply, they'd
   * expect Settings to be saved! So:
   * - Changing any setting in Settings immediately persists to DataStore AND updates `draftSettings`.
   * - Opening Preview lets user freely experiment with ALL appearance/chatbot settings inside the
   *   Preview Screen (modifying `draftSettings` live without overwriting saved settings yet), and
   *   then clicking **APPLY** commits `draftSettings` to DataStore, while clicking **CANCEL**
   *   reverts `draftSettings` back to `savedSettings`!
   * That gives the best of BOTH worlds: Settings always persist cleanly, AND Preview Mode lets you
   * experiment and test before clicking APPLY or CANCEL!
   */
  fun updateAndSaveSetting(transform: (AppSettings) -> AppSettings) {
    val updated = transform(_draftSettings.value)
    _draftSettings.value = updated
    _hasUnsavedDraftChanges.value = false
    viewModelScope.launch { settingsStore.saveSettings(updated) }
  }

  fun enterPreviewMode() {
    _draftSettings.value = savedSettings.value
    _isPreviewMode.value = true
  }

  fun updatePreviewDraft(transform: (AppSettings) -> AppSettings) {
    _hasUnsavedDraftChanges.value = true
    _draftSettings.update(transform)
  }

  fun applyPreviewChanges() {
    val toSave = _draftSettings.value
    _isPreviewMode.value = false
    _hasUnsavedDraftChanges.value = false
    viewModelScope.launch { settingsStore.saveSettings(toSave) }
  }

  fun cancelPreviewChanges() {
    _draftSettings.value = savedSettings.value
    _isPreviewMode.value = false
    _hasUnsavedDraftChanges.value = false
  }

  fun resetAllSettings() {
    val defaults = AppSettings()
    _draftSettings.value = defaults
    _hasUnsavedDraftChanges.value = false
    viewModelScope.launch { settingsStore.resetToDefaults() }
  }
}
