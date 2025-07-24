package com.bwc.tul.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

// Room Database Entities
@Entity(
    tableName = "sessions",
    indices = [Index(value = ["startTime"])]
)
data class ConversationSession(
    @PrimaryKey val id: Long = System.currentTimeMillis(),
    val startTime: Date = Date()
)

@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["timestamp"])
    ],
    foreignKeys = [ForeignKey(
        entity = ConversationSession::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class TranslationEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Long,
    val englishText: String,
    val thaiText: String,
    val timestamp: Date = Date(),
    val isFromEnglish: Boolean
)

// Non-entity data classes for queries
data class SessionWithPreview(
    val id: Long,
    val startTime: Date,
    val previewText: String?
)

data class SessionPreview(
    val session: ConversationSession,
    val previewText: String
)