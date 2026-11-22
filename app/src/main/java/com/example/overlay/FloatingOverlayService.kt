package com.example.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ROUTE_SCANNER
import com.example.data.local.db.AppDatabase
import com.example.data.local.prefs.SettingsDataStore
import com.example.data.ocr.OcrProcessor
import com.example.data.remote.NetworkModule
import com.example.data.repository.AiSolverRepository
import com.example.data.repository.AiStreamEvent
import com.example.model.AppSettings
import com.example.model.ChatWindowSize
import com.example.model.FloatingPosition
import com.example.model.SessionSource
import com.example.model.SubjectTag
import com.example.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingOverlayService : Service() {

  companion object {
    private const val CHANNEL_ID = "ai_solver_overlay_channel"
    private const val NOTIFICATION_ID = 4102
    const val ACTION_STOP_OVERLAY = "com.example.overlay.ACTION_STOP"

    private val _isRunningFlow = MutableStateFlow(false)
    val isRunningFlow: StateFlow<Boolean> = _isRunningFlow.asStateFlow()
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private lateinit var windowManager: WindowManager
  private lateinit var settingsStore: SettingsDataStore
  private lateinit var repository: AiSolverRepository
  private lateinit var ocrProcessor: OcrProcessor

  private var currentSettings = AppSettings()
  private var isExpanded = false
  private var windowSizeOverride: ChatWindowSize? = null
  private var isHiddenForCapture = false

  private var bubbleView: View? = null
  private var bubbleParams: WindowManager.LayoutParams? = null
  private var panelView: View? = null
  private var panelParams: WindowManager.LayoutParams? = null

  private var messagesContainer: LinearLayout? = null
  private var messagesScrollView: ScrollView? = null
  private var thinkingBanner: TextView? = null
  private var streamingTextView: TextView? = null
  private var inputEditText: EditText? = null
  private var sendButtonView: TextView? = null

  private var activeSessionId: Long? = null
  private var requestJob: Job? = null
  private val overlayMessages = mutableListOf<Pair<Boolean, String>>() // true = user, false = AI

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    if (!OverlayHelper.canDrawOverlays(this)) {
      stopSelf()
      return
    }
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    settingsStore = SettingsDataStore(this)
    val dao = AppDatabase.getInstance(this).chatDao()
    repository = AiSolverRepository(this, NetworkModule.aiApiService, dao)
    ocrProcessor = OcrProcessor(this)

    startAsForeground()
    _isRunningFlow.value = true

    serviceScope.launch {
      ScreenCaptureBus.results.collect { result ->
        handleScreenCaptureResult(result)
      }
    }

    serviceScope.launch {
      currentSettings = settingsStore.settingsFlow.first()
      showFloatingBubble()
      settingsStore.settingsFlow.collect { updated ->
        currentSettings = updated
        applyUpdatedSettings()
      }
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP_OVERLAY) {
      serviceScope.launch {
        settingsStore.saveSettings(currentSettings.copy(systemOverlayEnabled = false))
        stopSelf()
      }
      return START_NOT_STICKY
    }
    return START_STICKY
  }

  override fun onDestroy() {
    _isRunningFlow.value = false
    requestJob?.cancel()
    removeBubble()
    removePanel()
    serviceScope.cancel()
    super.onDestroy()
  }

  private fun startAsForeground() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
            CHANNEL_ID,
            "AI Solver Layar Mengambang",
            NotificationManager.IMPORTANCE_LOW,
          )
          .apply { description = "Menampilkan asisten AI Solver di atas aplikasi lain" }
      val nm = getSystemService(NotificationManager::class.java)
      nm.createNotificationChannel(channel)
    }

    val openAppIntent =
      PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    val stopIntent =
      PendingIntent.getService(
        this,
        1,
        Intent(this, FloatingOverlayService::class.java).apply { action = ACTION_STOP_OVERLAY },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
      )

    val notification: Notification =
      NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("AI Solver Aktif di Atas Aplikasi")
        .setContentText("Ketuk gelembung mengambang untuk tanya soal cepat")
        .setSmallIcon(R.drawable.ic_launcher_art)
        .setOngoing(true)
        .setContentIntent(openAppIntent)
        .addAction(0, "Matikan", stopIntent)
        .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(
        NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
      )
    } else {
      startForeground(NOTIFICATION_ID, notification)
    }
  }

  private fun overlayWindowType(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }
  }

  private fun isDarkActive(): Boolean {
    return when (currentSettings.themeMode) {
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
      ThemeMode.SYSTEM ->
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
          Configuration.UI_MODE_NIGHT_YES
    }
  }

  private fun applyUpdatedSettings() {
    bubbleView?.alpha = currentSettings.floatingOpacity
    panelView?.alpha = currentSettings.floatingOpacity.coerceIn(0.84f, 1.0f)
    if (isExpanded) {
      removePanel()
      showExpandedPanel()
    }
  }

  private fun showFloatingBubble() {
    if (bubbleView != null) return
    val sizePx = dp(58)
    val metrics = resources.displayMetrics

    val bubbleContainer =
      FrameLayout(this).apply {
        alpha = currentSettings.floatingOpacity
        background =
          GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#6C47FF"), Color.parseColor("#0DB394")),
          ).apply {
            shape = GradientDrawable.OVAL
            setStroke(dp(2), Color.WHITE)
          }
        elevation = dp(10).toFloat()

        val icon =
          ImageView(this@FloatingOverlayService).apply {
            setImageResource(R.drawable.ic_launcher_art)
            scaleType = ImageView.ScaleType.CENTER_CROP
            val pad = dp(10)
            setPadding(pad, pad, pad, pad)
          }
        addView(
          icon,
          FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
          ),
        )
      }

    val isLeft =
      currentSettings.floatingPosition == FloatingPosition.BOTTOM_LEFT ||
        currentSettings.floatingPosition == FloatingPosition.MIDDLE_LEFT
    val isMiddle =
      currentSettings.floatingPosition == FloatingPosition.MIDDLE_LEFT ||
        currentSettings.floatingPosition == FloatingPosition.MIDDLE_RIGHT

    val params =
      WindowManager.LayoutParams(
          sizePx,
          sizePx,
          overlayWindowType(),
          WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
          PixelFormat.TRANSLUCENT,
        )
        .apply {
          gravity = Gravity.TOP or Gravity.START
          x = if (isLeft) dp(16) else (metrics.widthPixels - sizePx - dp(16))
          y = if (isMiddle) metrics.heightPixels / 2 - sizePx else (metrics.heightPixels - dp(180))
        }

    var startX = 0
    var startY = 0
    var touchStartX = 0f
    var touchStartY = 0f
    var moved = false

    bubbleContainer.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          startX = params.x
          startY = params.y
          touchStartX = event.rawX
          touchStartY = event.rawY
          moved = false
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = (event.rawX - touchStartX).toInt()
          val dy = (event.rawY - touchStartY).toInt()
          if (abs(dx) > 8 || abs(dy) > 8) moved = true
          params.x = (startX + dx).coerceIn(0, metrics.widthPixels - sizePx)
          params.y = (startY + dy).coerceIn(dp(24), metrics.heightPixels - sizePx - dp(24))
          runCatching { windowManager.updateViewLayout(bubbleContainer, params) }
          true
        }
        MotionEvent.ACTION_UP -> {
          if (!moved) {
            expandToPanel()
          } else {
            // Snap softly to nearest horizontal edge
            params.x =
              if (params.x + sizePx / 2 < metrics.widthPixels / 2) dp(14)
              else metrics.widthPixels - sizePx - dp(14)
            runCatching { windowManager.updateViewLayout(bubbleContainer, params) }
          }
          true
        }
        else -> false
      }
    }

    bubbleView = bubbleContainer
    bubbleParams = params
    runCatching { windowManager.addView(bubbleContainer, params) }
  }

  private fun expandToPanel() {
    isExpanded = true
    removeBubble()
    showExpandedPanel()
  }

  private fun collapseToBubble() {
    isExpanded = false
    removePanel()
    showFloatingBubble()
  }

  private fun showExpandedPanel() {
    if (panelView != null) return
    val dark = isDarkActive()
    val cardBg = if (dark) Color.parseColor("#181C2B") else Color.parseColor("#FFFFFF")
    val headerBg = if (dark) Color.parseColor("#232840") else Color.parseColor("#F1F4FD")
    val borderColor = if (dark) Color.parseColor("#353C5C") else Color.parseColor("#DCE2F5")
    val textPrimary = if (dark) Color.WHITE else Color.parseColor("#121629")
    val textSecondary = if (dark) Color.parseColor("#B4BCD4") else Color.parseColor("#55607D")
    val inputBg = if (dark) Color.parseColor("#24293D") else Color.parseColor("#F4F6FC")
    val primaryColor = Color.parseColor("#6C47FF")
    val secondaryColor = Color.parseColor("#0DB394")

    val metrics = resources.displayMetrics
    val activeSize = windowSizeOverride ?: currentSettings.chatWindowSize
    val panelWidth = (metrics.widthPixels * 0.90f).toInt().coerceAtMost(dp(440))
    val panelHeight = (metrics.heightPixels * activeSize.heightFraction.coerceIn(0.42f, 0.78f)).toInt()

    val rootCard =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        alpha = currentSettings.floatingOpacity.coerceIn(0.86f, 1.0f)
        background =
          GradientDrawable().apply {
            setColor(cardBg)
            cornerRadius = dp(22).toFloat()
            setStroke(dp(1), borderColor)
          }
        elevation = dp(16).toFloat()
        clipToOutline = true
      }

    // Header (Draggable)
    val header =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(10), dp(10), dp(10))
        setBackgroundColor(headerBg)
      }

    val titleCol =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
      }
    val titleTxt =
      TextView(this).apply {
        text = "AI Solver Mengambang"
        setTextColor(textPrimary)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setTypeface(null, Typeface.BOLD)
      }
    val subtitleTxt =
      TextView(this).apply {
        text = "Aktif di atas aplikasi lain • Geser header"
        setTextColor(textSecondary)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      }
    titleCol.addView(titleTxt)
    titleCol.addView(subtitleTxt)
    header.addView(titleCol)

    fun makeHeaderBtn(label: String, bg: Int, fg: Int, onClick: () -> Unit): TextView {
      return TextView(this).apply {
        text = label
        setTextColor(fg)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(9), dp(6), dp(9), dp(6))
        background =
          GradientDrawable().apply {
            setColor(bg)
            cornerRadius = dp(10).toFloat()
          }
        val lp =
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
          ).apply { marginStart = dp(5) }
        layoutParams = lp
        setOnClickListener { onClick() }
      }
    }

    val chipBg = if (dark) Color.parseColor("#2E3552") else Color.parseColor("#E5EAFA")
    header.addView(
      makeHeaderBtn("Ukuran", chipBg, primaryColor) {
        val current = windowSizeOverride ?: currentSettings.chatWindowSize
        windowSizeOverride =
          when (current) {
            ChatWindowSize.COMPACT -> ChatWindowSize.STANDARD
            ChatWindowSize.STANDARD -> ChatWindowSize.EXPANDED
            ChatWindowSize.EXPANDED -> ChatWindowSize.COMPACT
          }
        removePanel()
        showExpandedPanel()
      }
    )
    header.addView(
      makeHeaderBtn("Hapus", chipBg, textSecondary) {
        overlayMessages.clear()
        activeSessionId = null
        refreshMessagesUi(dark, textPrimary, textSecondary)
      }
    )
    header.addView(
      makeHeaderBtn("Lipat", primaryColor, Color.WHITE) { collapseToBubble() }
    )
    header.addView(
      makeHeaderBtn("OFF", Color.parseColor("#FFE5EC"), Color.parseColor("#D92D53")) {
        serviceScope.launch {
          settingsStore.saveSettings(currentSettings.copy(systemOverlayEnabled = false))
          stopSelf()
        }
      }
    )

    rootCard.addView(header)

    // Quick Action Toolbar inside Floating Window: Scan Soal & Upload Soal
    val quickActionRow =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(12), dp(8), dp(12), dp(6))
        setBackgroundColor(cardBg)
      }

    fun makeActionTile(
      label: String,
      bgHex: String,
      strokeHex: String,
      fgHex: String,
      marginEndDp: Int,
      onClick: () -> Unit,
    ): TextView {
      return TextView(this).apply {
        text = label
        setTextColor(Color.parseColor(fgHex))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(10), dp(9), dp(10), dp(9))
        background =
          GradientDrawable().apply {
            setColor(Color.parseColor(bgHex))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.parseColor(strokeHex))
          }
        layoutParams =
          LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            if (marginEndDp > 0) marginEnd = dp(marginEndDp)
          }
        setOnClickListener { onClick() }
      }
    }

    quickActionRow.addView(
      makeActionTile(
        label = "📸 Scan Layar (Auto)",
        bgHex = if (dark) "#282356" else "#EDE8FF",
        strokeHex = if (dark) "#5748BA" else "#C9BAFF",
        fgHex = if (dark) "#DFD6FF" else "#5028E6",
        marginEndDp = 8,
      ) {
        startAutoScreenshotScan()
      }
    )

    quickActionRow.addView(
      makeActionTile(
        label = "📷 Kamera / Galeri",
        bgHex = if (dark) "#133C34" else "#D9F7EF",
        strokeHex = if (dark) "#20A88A" else "#9BE7D5",
        fgHex = if (dark) "#ADF5E4" else "#057A62",
        marginEndDp = 0,
      ) {
        collapseToBubble()
        val scanIntent =
          Intent(this@FloatingOverlayService, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_ROUTE, ROUTE_SCANNER)
            putExtra(MainActivity.EXTRA_AUTO_PICK_GALLERY, false)
          }
        startActivity(scanIntent)
      }
    )

    rootCard.addView(quickActionRow)

    // Messages Scroll Area
    val scrollView =
      ScrollView(this).apply {
        layoutParams =
          LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        setPadding(dp(12), dp(10), dp(12), dp(10))
      }
    val msgCol =
      LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
      }
    scrollView.addView(msgCol)
    messagesScrollView = scrollView
    messagesContainer = msgCol
    rootCard.addView(scrollView)

    // Thinking indicator & streaming area
    val thinkingTv =
      TextView(this).apply {
        text = "✨ AI sedang berpikir..."
        setTextColor(primaryColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTypeface(null, Typeface.BOLD)
        setPadding(dp(14), dp(4), dp(14), dp(4))
        visibility = View.GONE
      }
    thinkingBanner = thinkingTv
    rootCard.addView(thinkingTv)

    val streamTv =
      TextView(this).apply {
        setTextColor(textPrimary)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setPadding(dp(14), dp(4), dp(14), dp(8))
        visibility = View.GONE
      }
    streamingTextView = streamTv
    rootCard.addView(streamTv)

    // Input Bar
    val inputRow =
      LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(8), dp(10), dp(10))
        setBackgroundColor(headerBg)
      }

    val editText =
      EditText(this).apply {
        hint = "Tulis soal / pertanyaan..."
        setHintTextColor(textSecondary)
        setTextColor(textPrimary)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        maxLines = 3
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        imeOptions = EditorInfo.IME_ACTION_SEND
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background =
          GradientDrawable().apply {
            setColor(inputBg)
            cornerRadius = dp(16).toFloat()
            setStroke(dp(1), borderColor)
          }
        layoutParams =
          LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(8)
          }
      }
    inputEditText = editText

    val sendBtn =
      TextView(this).apply {
        text = "Kirim"
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(11), dp(14), dp(11))
        background =
          GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(primaryColor, secondaryColor),
          ).apply { cornerRadius = dp(14).toFloat() }
        setOnClickListener {
          val q = editText.text.toString().trim()
          if (q.isNotEmpty()) {
            editText.setText("")
            sendOverlayQuestion(q, dark, textPrimary, textSecondary)
          }
        }
      }
    sendButtonView = sendBtn

    editText.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_SEND) {
        sendBtn.performClick()
        true
      } else {
        false
      }
    }

    inputRow.addView(editText)
    inputRow.addView(sendBtn)
    rootCard.addView(inputRow)

    // Render existing messages
    refreshMessagesUi(dark, textPrimary, textSecondary)

    val params =
      WindowManager.LayoutParams(
          panelWidth,
          panelHeight,
          overlayWindowType(),
          WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
          PixelFormat.TRANSLUCENT,
        )
        .apply {
          gravity = Gravity.TOP or Gravity.START
          x = (metrics.widthPixels - panelWidth) / 2
          y = dp(64)
          softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

    // Drag header to reposition floating panel
    var pStartX = 0
    var pStartY = 0
    var tStartX = 0f
    var tStartY = 0f
    header.setOnTouchListener { _, ev ->
      when (ev.action) {
        MotionEvent.ACTION_DOWN -> {
          pStartX = params.x
          pStartY = params.y
          tStartX = ev.rawX
          tStartY = ev.rawY
          true
        }
        MotionEvent.ACTION_MOVE -> {
          params.x = (pStartX + (ev.rawX - tStartX).toInt()).coerceIn(0, metrics.widthPixels - panelWidth)
          params.y = (pStartY + (ev.rawY - tStartY).toInt()).coerceIn(dp(16), metrics.heightPixels - panelHeight / 2)
          runCatching { windowManager.updateViewLayout(rootCard, params) }
          true
        }
        else -> false
      }
    }

    panelView = rootCard
    panelParams = params
    runCatching {
      windowManager.addView(rootCard, params)
      editText.requestFocus()
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      editText.postDelayed({ imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT) }, 120)
    }
  }

  private fun startAutoScreenshotScan() {
    setOverlayHiddenForCapture(true)
    val intent =
      Intent(this, ScreenCaptureActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
      }
    startActivity(intent)
  }

  private fun setOverlayHiddenForCapture(hidden: Boolean) {
    isHiddenForCapture = hidden
    bubbleView?.visibility = if (hidden) View.GONE else View.VISIBLE
    panelView?.visibility = if (hidden) View.GONE else View.VISIBLE
  }

  private fun handleScreenCaptureResult(result: ScreenCaptureResult) {
    setOverlayHiddenForCapture(false)
    if (!isExpanded) {
      expandToPanel()
    }
    val dark = isDarkActive()
    val textPrimary = if (dark) Color.WHITE else Color.parseColor("#121629")
    val textSecondary = if (dark) Color.parseColor("#B4BCD4") else Color.parseColor("#55607D")

    when (result) {
      is ScreenCaptureResult.CancelledOrFailed -> {
        Toast.makeText(this, result.reason, Toast.LENGTH_SHORT).show()
      }
      is ScreenCaptureResult.Success -> {
        requestJob?.cancel()
        requestJob =
          serviceScope.launch {
            thinkingBanner?.text = "🔍 Membaca teks soal dari screenshot..."
            thinkingBanner?.visibility = View.VISIBLE
            streamingTextView?.visibility = View.GONE
            sendButtonView?.isEnabled = false

            val ocrText =
              try {
                kotlinx.coroutines.withContext(Dispatchers.Default) {
                  ocrProcessor.recognizeText(result.bitmap)
                }
              } catch (e: Exception) {
                ""
              } finally {
                runCatching { result.bitmap.recycle() }
              }

            if (ocrText.isBlank()) {
              thinkingBanner?.visibility = View.GONE
              sendButtonView?.isEnabled = true
              Toast.makeText(
                  this@FloatingOverlayService,
                  "Tidak ada teks soal yang terbaca pada screenshot.",
                  Toast.LENGTH_LONG,
                )
                .show()
              return@launch
            }

            // Automatically convert screenshot OCR text into a prompt and send to AI
            val displayQuestion = "📸 Hasil Scan Layar:\n$ocrText"
            overlayMessages.add(true to displayQuestion)
            refreshMessagesUi(dark, textPrimary, textSecondary)

            thinkingBanner?.text = "✨ Mengubah jadi prompt & mengirim ke Claude AI..."
            thinkingBanner?.visibility = View.VISIBLE

            val prompt = repository.buildHomeworkPrompt(ocrText, SubjectTag.ALL)
            val sid =
              activeSessionId
                ?: repository.createSession(ocrText, SessionSource.CAMERA_SCAN).also {
                  activeSessionId = it
                }
            repository.saveUserMessage(sid, displayQuestion, ocrText = ocrText)

            repository.askAiStreaming(
                prompt,
                animateStreaming = currentSettings.chatAnimationEnabled,
              )
              .collect { event ->
                when (event) {
                  is AiStreamEvent.Partial -> {
                    streamingTextView?.visibility = View.VISIBLE
                    streamingTextView?.text = event.text
                    scrollToBottom()
                  }
                  is AiStreamEvent.Completed -> {
                    thinkingBanner?.visibility = View.GONE
                    thinkingBanner?.text = "✨ AI sedang berpikir..."
                    streamingTextView?.visibility = View.GONE
                    sendButtonView?.isEnabled = true
                    overlayMessages.add(false to event.fullText)
                    repository.saveAssistantMessage(sid, event.fullText, isError = false)
                    refreshMessagesUi(dark, textPrimary, textSecondary)
                  }
                  is AiStreamEvent.Error -> {
                    thinkingBanner?.visibility = View.GONE
                    thinkingBanner?.text = "✨ AI sedang berpikir..."
                    streamingTextView?.visibility = View.GONE
                    sendButtonView?.isEnabled = true
                    Toast.makeText(this@FloatingOverlayService, event.message, Toast.LENGTH_LONG)
                      .show()
                  }
                }
              }
          }
      }
    }
  }

  private fun sendOverlayQuestion(
    question: String,
    dark: Boolean,
    textPrimary: Int,
    textSecondary: Int,
  ) {
    overlayMessages.add(true to question)
    refreshMessagesUi(dark, textPrimary, textSecondary)

    requestJob?.cancel()
    requestJob =
      serviceScope.launch {
        thinkingBanner?.text = "✨ AI sedang berpikir..."
        thinkingBanner?.visibility = View.VISIBLE
        streamingTextView?.visibility = View.GONE
        sendButtonView?.isEnabled = false

        val sid =
          activeSessionId
            ?: repository.createSession(question, SessionSource.CHAT).also { activeSessionId = it }
        repository.saveUserMessage(sid, question)

        repository.askAiStreaming(question, animateStreaming = currentSettings.chatAnimationEnabled)
          .collect { event ->
            when (event) {
              is AiStreamEvent.Partial -> {
                streamingTextView?.visibility = View.VISIBLE
                streamingTextView?.text = event.text
                scrollToBottom()
              }
              is AiStreamEvent.Completed -> {
                thinkingBanner?.visibility = View.GONE
                streamingTextView?.visibility = View.GONE
                sendButtonView?.isEnabled = true
                overlayMessages.add(false to event.fullText)
                repository.saveAssistantMessage(sid, event.fullText, isError = false)
                refreshMessagesUi(dark, textPrimary, textSecondary)
              }
              is AiStreamEvent.Error -> {
                thinkingBanner?.visibility = View.GONE
                streamingTextView?.visibility = View.GONE
                sendButtonView?.isEnabled = true
                Toast.makeText(this@FloatingOverlayService, event.message, Toast.LENGTH_LONG).show()
              }
            }
          }
      }
  }

  private fun refreshMessagesUi(dark: Boolean, textPrimary: Int, textSecondary: Int) {
    val container = messagesContainer ?: return
    container.removeAllViews()

    if (overlayMessages.isEmpty()) {
      val emptyTv =
        TextView(this).apply {
          text =
            "Halo! Saya mengambang di atas aplikasi Anda.\nKetik pertanyaan soal apa saja di bawah untuk dijawab langsung."
          setTextColor(textSecondary)
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
          setPadding(dp(8), dp(20), dp(8), dp(20))
          gravity = Gravity.CENTER
        }
      container.addView(emptyTv)
      return
    }

    val userBg = Color.parseColor("#6C47FF")
    val aiBg = if (dark) Color.parseColor("#24293D") else Color.parseColor("#F4F6FC")
    val aiBorder = if (dark) Color.parseColor("#353C5C") else Color.parseColor("#DCE2F5")

    overlayMessages.forEach { (isUser, content) ->
      val bubble =
        LinearLayout(this).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(dp(12), dp(9), dp(12), dp(9))
          background =
            GradientDrawable().apply {
              setColor(if (isUser) userBg else aiBg)
              cornerRadius = dp(14).toFloat()
              if (!isUser) setStroke(dp(1), aiBorder)
            }
          layoutParams =
            LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT,
              LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
              bottomMargin = dp(8)
              if (isUser) marginStart = dp(42) else marginEnd = dp(18)
            }
        }

      val tv =
        TextView(this).apply {
          text = content
          setTextColor(if (isUser) Color.WHITE else textPrimary)
          setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
        }
      bubble.addView(tv)

      if (!isUser) {
        val actions =
          LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
          }
        fun miniAction(label: String, onTap: () -> Unit): TextView {
          return TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#0DB394"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener { onTap() }
          }
        }
        actions.addView(
          miniAction("Salin") {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("AI Solver", content))
            Toast.makeText(this@FloatingOverlayService, "Disalin", Toast.LENGTH_SHORT).show()
          }
        )
        actions.addView(
          miniAction("Bagikan") {
            val shareIntent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
            startActivity(
              Intent.createChooser(shareIntent, "Bagikan jawaban")
                .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
          }
        )
        bubble.addView(actions)
      }

      container.addView(bubble)
    }

    scrollToBottom()
  }

  private fun scrollToBottom() {
    messagesScrollView?.post { messagesScrollView?.fullScroll(View.FOCUS_DOWN) }
  }

  private fun removeBubble() {
    bubbleView?.let {
      runCatching { windowManager.removeView(it) }
      bubbleView = null
      bubbleParams = null
    }
  }

  private fun removePanel() {
    panelView?.let {
      runCatching { windowManager.removeView(it) }
      panelView = null
      panelParams = null
    }
  }

  private fun dp(value: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics,
      )
      .toInt()
  }
}
