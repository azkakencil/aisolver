package com.example.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
  @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC")
  fun observeSessions(): Flow<List<ChatSessionEntity>>

  @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC, id ASC")
  fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
  fun observeAllMessages(): Flow<List<ChatMessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSession(session: ChatSessionEntity): Long

  @Query("UPDATE chat_sessions SET updatedAt = :updatedAt, title = :title WHERE id = :sessionId")
  suspend fun updateSessionMeta(sessionId: Long, title: String, updatedAt: Long)

  @Query("UPDATE chat_sessions SET updatedAt = :updatedAt WHERE id = :sessionId")
  suspend fun touchSession(sessionId: Long, updatedAt: Long)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: ChatMessageEntity): Long

  @Query("UPDATE chat_messages SET content = :content, isError = :isError, timestamp = :timestamp WHERE id = :messageId")
  suspend fun updateMessage(messageId: Long, content: String, isError: Boolean, timestamp: Long)

  @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
  suspend fun deleteMessagesForSession(sessionId: Long)

  @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
  suspend fun deleteSessionById(sessionId: Long)

  @Query("DELETE FROM chat_sessions")
  suspend fun deleteAllSessions()
}
