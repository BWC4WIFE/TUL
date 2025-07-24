package com.bwc.tul.data


import androidx.room.*
import com.bwc.tul.data.ConversationSession
import com.bwc.tul.data.SessionWithPreview
import com.bwc.tul.data.TranslationEntry
import com.bwc.tul.data.websocket.WebSocketLogEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConversationSession): Long

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ConversationSession>>

    @Transaction
    @Query("""
        SELECT s.id, s.startTime, 
        (SELECT COALESCE(
            CASE WHEN e.isFromEnglish THEN e.englishText ELSE e.thaiText END,
            'No messages'
        )
        FROM entries e 
        WHERE e.sessionId = s.id 
        ORDER BY e.timestamp ASC 
        LIMIT 1) as previewText
        FROM sessions s
        ORDER BY s.startTime DESC
    """)
    fun getSessionsWithPreviews(): Flow<List<SessionWithPreview>>
}

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TranslationEntry)

    @Query("SELECT * FROM entries WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEntriesForSession(sessionId: Long): Flow<List<TranslationEntry>>
}

@Dao
interface WebSocketLogDao {
    @Insert
    suspend fun insert(logEntry: WebSocketLogEntry)

    @Query("SELECT * FROM websocket_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentLogs(limit: Int = 200): List<WebSocketLogEntry>

    @Query("SELECT * FROM websocket_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<WebSocketLogEntry>

    @Query("DELETE FROM websocket_logs WHERE timestamp < :cutoffTime")
    suspend fun deleteOldLogs(cutoffTime: Long)
}