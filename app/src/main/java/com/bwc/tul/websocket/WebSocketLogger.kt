package com.bwc.tul.data.websocket


import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.*
import android.content.Context
import com.bwc.tul.data.AppDatabase
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.bwc.tul.websocket.*


class WebSocketLogger(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).webSocketLogDao()

    suspend fun logSentMessage(url: String, message: String) = withContext(Dispatchers.IO) {
        dao.insert(
            WebSocketLogEntry(
                direction = WebSocketLogEntry.Direction.SENT,
                url = url,
                message = message
            )
        )
    }

    suspend fun logReceivedMessage(url: String, message: String) = withContext(Dispatchers.IO) {
        dao.insert(
            WebSocketLogEntry(
                direction = WebSocketLogEntry.Direction.RECEIVED,
                url = url,
                message = if (isAudioMessage(message)) "[AUDIO DATA]" else message
            )
        )
    }

    suspend fun getLogs(limit: Int = 200): List<WebSocketLogEntry> = withContext(Dispatchers.IO) {
        dao.getRecentLogs(limit)
    }

    suspend fun exportLogs(): File = withContext(Dispatchers.IO) {
        val logs = dao.getAllLogs()
        val file = File(context.cacheDir, "websocket_logs_${System.currentTimeMillis()}.txt")

        file.writeText(buildString {
            logs.forEach { log ->
                appendLine("[${Date(log.timestamp)}] ${log.direction} ${log.url}")
                if (log.isError) {
                    appendLine("ERROR: ${log.errorMessage}")
                }
                appendLine(log.message)
                appendLine("=".repeat(80))
            }
        })
        file
    }

    private fun isAudioMessage(message: String): Boolean {
        return try {
            if (message.startsWith("{")) {
                val json = JSONObject(message)
                json.has("audio") || json.optString("contentType").contains("audio", ignoreCase = true)
            } else {
                message.contains("content-type: audio", ignoreCase = true)
            }
        } catch (e: Exception) {
            false
        }
    }
}