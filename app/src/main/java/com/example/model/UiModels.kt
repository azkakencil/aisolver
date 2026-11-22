package com.example.model

enum class SubjectTag(val label: String) {
  ALL("Umum"),
  MATH("Matematika"),
  PHYSICS("Fisika"),
  CHEMISTRY("Kimia"),
  LANGUAGE("Bahasa"),
  HISTORY("Sejarah"),
  CODING("Informatika"),
}

enum class MessageRole {
  USER,
  ASSISTANT,
}

enum class SessionSource(val label: String) {
  CHAT("Chat AI"),
  CAMERA_SCAN("Scan Kamera"),
  GALLERY_SCAN("Upload Gambar"),
}

data class ChatMessageUi(
  val id: Long,
  val sessionId: Long,
  val role: MessageRole,
  val content: String,
  val ocrText: String? = null,
  val subjectTag: String? = null,
  val isError: Boolean = false,
  val timestamp: Long = System.currentTimeMillis(),
)

data class ChatSessionSummary(
  val id: Long,
  val title: String,
  val lastQuestion: String,
  val lastAnswer: String,
  val source: SessionSource,
  val subjectTag: String,
  val updatedAt: Long,
)

data class ScanState(
  val isProcessingOcr: Boolean = false,
  val isSolving: Boolean = false,
  val recognizedText: String = "",
  val solutionText: String = "",
  val ocrError: String? = null,
  val solveError: String? = null,
  val selectedSubject: SubjectTag = SubjectTag.ALL,
  val activeSessionId: Long? = null,
)
