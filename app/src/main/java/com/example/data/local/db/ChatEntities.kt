package com.example.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val sourceType: String,
  val subjectTag: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "chat_messages",
  foreignKeys = [
    ForeignKey(
      entity = ChatSessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["sessionId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index("sessionId")],
)
data class ChatMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val sessionId: Long,
  val role: String,
  val content: String,
  val ocrText: String? = null,
  val subjectTag: String? = null,
  val isError: Boolean = false,
  val timestamp: Long = System.currentTimeMillis(),
)
