package com.bwc.tul.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.bwc.tul.data.AppDatabase
import com.bwc.tul.data.LogDao
import com.bwc.tul.data.LogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A logger object that writes logs to a Room database and provides
 * functionality to export them to a file.
 */
object DebugLogger {
    private const val TAG = "DebugLogger"
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var logDao: LogDao

    /**
     * Initializes the DebugLogger with a LogDao instance.
     * Must be called before using logging functions.
     */
    fun initialize(context: Context) {
        logDao = AppDatabase.getDatabase(context).logDao()
    }

    /**
     * Logs a message to the Room database.
     * @param level The severity level of the log (e.g., "INFO", "DEBUG").
     * @param tag The tag for the log message.
     * @param message The content of the log message.
     */
    fun log(level: String, tag: String, message: String) {
        if (!::logDao.isInitialized) {
            Log.e(TAG, "DebugLogger not initialized. Call initialize() first.")
            return
        }

        // Use a coroutine to avoid blocking the main thread
        scope.launch {
            try {
                val logEntry = LogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = level,
                    tag = tag,
                    message = message
                )
                logDao.insert(logEntry)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log to database", e)
            }
        }

        // Print to Logcat for real-time debugging
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val logcatMessage = "[$timestamp] [$level/$tag]: $message"
        when (level) {
            "ERROR" -> Log.e(tag, logcatMessage)
            "WARN" -> Log.w(tag, logcatMessage)
            else -> Log.i(tag, logcatMessage)
        }
    }

    /**
     * Clears all logs from the database.
     */
    fun clear() {
        if (!::logDao.isInitialized) {
            Log.e(TAG, "DebugLogger not initialized. Call initialize() first.")
            return
        }

        scope.launch {
            try {
                logDao.clearAll()
                log("INFO", TAG, "--- Log Cleared ---")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs from database", e)
            }
        }
    }

    /**
     * Retrieves all logs from the database, writes them to a text file,
     * and returns a content URI for sharing.
     */
    suspend fun getLogFileUri(context: Context): Uri? {
        if (!::logDao.isInitialized) {
            Log.e(TAG, "DebugLogger not initialized. Call initialize() first.")
            return null
        }

        return try {
            val allLogs = logDao.getLogs()
            val logText = buildString {
                allLogs.forEach { entry ->
                    val date = SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss.SSS",
                        Locale.US
                    ).format(Date(entry.timestamp))
                    appendLine("[$date] ${entry.level}/${entry.tag}: ${entry.message}")
                    appendLine("-".repeat(70))
                }
            }

            val logDir = File(context.cacheDir, "logs")
            logDir.mkdirs()
            val logFile = File(logDir, "debug_log_${System.currentTimeMillis()}.txt")
            logFile.writeText(logText)

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                logFile
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating log file URI", e)
            null
        }
    }
}