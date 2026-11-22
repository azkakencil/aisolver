package com.example.data.repository

import android.content.Context
import com.example.data.local.db.ChatDao
import com.example.data.local.db.ChatMessageEntity
import com.example.data.local.db.ChatSessionEntity
import com.example.data.remote.AiApiService
import com.example.data.remote.ApiResponseException
import com.example.data.remote.Gpt4ResponseParser
import com.example.data.remote.NetworkModule
import com.example.model.ApiConnectionStatus
import com.example.model.ChatMessageUi
import com.example.model.ChatSessionSummary
import com.example.model.MessageRole
import com.example.model.SessionSource
import com.example.model.SubjectTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AiStreamEvent {
  data class Partial(val text: String) : AiStreamEvent()

  data class Completed(val fullText: String) : AiStreamEvent()

  data class Error(val message: String) : AiStreamEvent()
}

class AiSolverRepository(
  private val context: Context,
  private val apiService: AiApiService,
  private val chatDao: ChatDao,
) {

  val sessionSummaries: Flow<List<ChatSessionSummary>> =
    combine(chatDao.observeSessions(), chatDao.observeAllMessages()) { sessions, messages ->
      val messagesBySession = messages.groupBy { it.sessionId }
      sessions.map { session ->
        val sessionMsgs =
          messagesBySession[session.id].orEmpty().sortedBy { it.timestamp }
        val lastUser =
          sessionMsgs.lastOrNull { it.role == MessageRole.USER.name }?.content ?: session.title
        val lastAi =
          sessionMsgs.lastOrNull { it.role == MessageRole.ASSISTANT.name && !it.isError }?.content
            ?: ""
        ChatSessionSummary(
          id = session.id,
          title = session.title,
          lastQuestion = lastUser,
          lastAnswer = lastAi,
          source =
            runCatching { SessionSource.valueOf(session.sourceType) }
              .getOrDefault(SessionSource.CHAT),
          subjectTag = session.subjectTag,
          updatedAt = session.updatedAt,
        )
      }
    }

  fun observeMessages(sessionId: Long): Flow<List<ChatMessageUi>> =
    chatDao.observeMessagesForSession(sessionId).map { list ->
      list.map { entity ->
        ChatMessageUi(
          id = entity.id,
          sessionId = entity.sessionId,
          role =
            runCatching { MessageRole.valueOf(entity.role) }.getOrDefault(MessageRole.ASSISTANT),
          content = entity.content,
          ocrText = entity.ocrText,
          subjectTag = entity.subjectTag,
          isError = entity.isError,
          timestamp = entity.timestamp,
        )
      }
    }

  suspend fun createSession(
    title: String,
    source: SessionSource,
    subjectTag: SubjectTag = SubjectTag.ALL,
  ): Long {
    val now = System.currentTimeMillis()
    return chatDao.insertSession(
      ChatSessionEntity(
        title = title.take(80).ifBlank { "Sesi AI Solver" },
        sourceType = source.name,
        subjectTag = subjectTag.label,
        createdAt = now,
        updatedAt = now,
      )
    )
  }

  suspend fun saveUserMessage(
    sessionId: Long,
    content: String,
    ocrText: String? = null,
    subjectTag: String? = null,
  ): Long {
    val now = System.currentTimeMillis()
    val msgId =
      chatDao.insertMessage(
        ChatMessageEntity(
          sessionId = sessionId,
          role = MessageRole.USER.name,
          content = content,
          ocrText = ocrText,
          subjectTag = subjectTag,
          isError = false,
          timestamp = now,
        )
      )
    chatDao.updateSessionMeta(sessionId, content.take(72), now)
    return msgId
  }

  suspend fun saveAssistantMessage(
    sessionId: Long,
    content: String,
    isError: Boolean = false,
  ): Long {
    val now = System.currentTimeMillis()
    val id =
      chatDao.insertMessage(
        ChatMessageEntity(
          sessionId = sessionId,
          role = MessageRole.ASSISTANT.name,
          content = content,
          isError = isError,
          timestamp = now,
        )
      )
    chatDao.touchSession(sessionId, now)
    return id
  }

  suspend fun updateAssistantMessage(messageId: Long, content: String, isError: Boolean = false) {
    chatDao.updateMessage(messageId, content, isError, System.currentTimeMillis())
  }

  suspend fun deleteSession(sessionId: Long) {
    chatDao.deleteSessionById(sessionId)
  }

  suspend fun clearSessionMessages(sessionId: Long) {
    chatDao.deleteMessagesForSession(sessionId)
  }

  suspend fun deleteAllHistory() {
    chatDao.deleteAllSessions()
  }

  suspend fun checkApiStatus(): ApiConnectionStatus =
    withContext(Dispatchers.IO) {
      if (!NetworkModule.isNetworkAvailable(context)) {
        return@withContext ApiConnectionStatus.Offline("Tidak ada koneksi internet")
      }
      val start = System.currentTimeMillis()
      try {
        val response = apiService.askAi("ping")
        val latency = (System.currentTimeMillis() - start).coerceAtLeast(1L)
        if (response.isSuccessful) {
          val raw = response.body()?.string().orEmpty()
          // Verify the payload isn't an HTML error page
          if (raw.contains("<!DOCTYPE html", ignoreCase = true) ||
            raw.contains("<html", ignoreCase = true)
          ) {
            ApiConnectionStatus.Offline("Endpoint API mengembalikan halaman error")
          } else {
            ApiConnectionStatus.Online(latency)
          }
        } else {
          ApiConnectionStatus.Offline("HTTP ${response.code()}")
        }
      } catch (_: SocketTimeoutException) {
        ApiConnectionStatus.Offline("Koneksi timeout")
      } catch (_: UnknownHostException) {
        ApiConnectionStatus.Offline("Server tidak ditemukan")
      } catch (e: IOException) {
        ApiConnectionStatus.Offline(e.localizedMessage ?: "Gangguan jaringan")
      } catch (e: Exception) {
        ApiConnectionStatus.Offline(e.localizedMessage ?: "Gagal terhubung")
      }
    }

  /**
   * Queries the GPT-4 GET endpoint asynchronously, handles streaming SSE if supported by the
   * server, and otherwise smoothly reveals chunks for responsive UI feedback. Never treats API
   * errors as AI answers.
   */
  fun askAiStreaming(
    prompt: String,
    animateStreaming: Boolean = true,
  ): Flow<AiStreamEvent> =
    flow {
      if (!NetworkModule.isNetworkAvailable(context)) {
        emit(
          AiStreamEvent.Error(
            "Tidak ada koneksi internet. Periksa Wi-Fi atau data seluler Anda lalu coba lagi."
          )
        )
        return@flow
      }

      try {
        val response = apiService.askAi(prompt.trim())
        if (!response.isSuccessful) {
          val errCode = response.code()
          val msg =
            when (errCode) {
              400 -> "Permintaan tidak valid (HTTP 400). Coba sederhanakan pertanyaan."
              408,
              504 -> "Server AI membutuhkan waktu terlalu lama (Timeout $errCode). Coba lagi."
              429 -> "Terlalu banyak permintaan ke server AI (HTTP 429). Tunggu sebentar."
              in 500..599 -> "Server AI sedang mengalami gangguan ($errCode). Coba beberapa saat lagi."
              else -> "Gagal menghubungi API AI (HTTP $errCode)."
            }
          emit(AiStreamEvent.Error(msg))
          return@flow
        }

        val body = response.body()
        if (body == null) {
          emit(AiStreamEvent.Error("Respons dari server AI kosong."))
          return@flow
        }

        val contentType = response.headers()["Content-Type"].orEmpty()
        if (contentType.contains("text/event-stream", ignoreCase = true)) {
          val source = body.source()
          val accumulated = StringBuilder()
          while (!source.exhausted()) {
            val line = source.readUtf8Line() ?: break
            val chunk = Gpt4ResponseParser.extractSseChunk(line)
            if (!chunk.isNullOrEmpty()) {
              accumulated.append(chunk)
              emit(AiStreamEvent.Partial(accumulated.toString()))
            }
          }
          val finalOutput = accumulated.toString().trim()
          if (finalOutput.isEmpty()) {
            emit(AiStreamEvent.Error("Streaming AI selesai tanpa konten jawaban."))
          } else {
            emit(AiStreamEvent.Completed(finalOutput))
          }
        } else {
          val rawString = body.string()
          val parsedAnswer = Gpt4ResponseParser.parseResponseBody(rawString)

          if (animateStreaming && parsedAnswer.length > 24) {
            val tokens = parsedAnswer.split(" ")
            val builder = StringBuilder()
            val step = (tokens.size / 22).coerceIn(1, 5)
            tokens.forEachIndexed { idx, word ->
              if (builder.isNotEmpty()) builder.append(' ')
              builder.append(word)
              if (idx % step == 0 || idx == tokens.lastIndex) {
                emit(AiStreamEvent.Partial(builder.toString()))
                delay(18L)
              }
            }
          }
          emit(AiStreamEvent.Completed(parsedAnswer))
        }
      } catch (e: ApiResponseException) {
        emit(AiStreamEvent.Error(e.message ?: "Terjadi kesalahan saat memproses respons API."))
      } catch (_: SocketTimeoutException) {
        emit(
          AiStreamEvent.Error(
            "Waktu tunggu koneksi ke server AI habis (Timeout). Coba kirim ulang pertanyaan."
          )
        )
      } catch (_: UnknownHostException) {
        emit(
          AiStreamEvent.Error(
            "Tidak dapat menjangkau server api.nexray.eu.cc. Periksa koneksi internet Anda."
          )
        )
      } catch (e: IOException) {
        emit(
          AiStreamEvent.Error(
            "Gangguan koneksi jaringan: ${e.localizedMessage ?: "Tidak dapat terhubung"}."
          )
        )
      } catch (e: Exception) {
        emit(
          AiStreamEvent.Error(
            "Gagal memproses respons AI: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}."
          )
        )
      }
    }.flowOn(Dispatchers.IO)

  fun buildHomeworkPrompt(ocrText: String, subject: SubjectTag): String {
    val subjectContext =
      if (subject == SubjectTag.ALL) "mata pelajaran terkait" else "mata pelajaran ${subject.label}"
    return "Kamu adalah tutor AI cerdas. Tolong selesaikan soal $subjectContext berikut dengan jawaban yang akurat dan berikan penjelasan langkah demi langkah yang mudah dipahami dalam bahasa Indonesia:\n\n$ocrText"
  }

  fun buildExplainPrompt(answer: String): String =
    "Jelaskan kembali jawaban berikut ini dengan langkah demi langkah yang lebih sederhana dan mudah dipahami siswa:\n\n$answer"

  fun buildSummarizePrompt(answer: String): String =
    "Ringkas jawaban berikut menjadi poin-poin inti yang singkat, padat, dan jelas:\n\n$answer"

  fun buildTranslatePrompt(answer: String, targetLanguage: String): String =
    "Terjemahkan jawaban berikut ke dalam bahasa $targetLanguage dengan mempertahankan istilah ilmiah dan format langkahnya:\n\n$answer"
}
