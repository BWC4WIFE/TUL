package com.bwc.tul.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


object DebugLogger {
    private const val TAG = "DebugLogger"
    private val logBuilder = StringBuilder()
    private var logFile: File? = null

    @Synchronized
    fun log(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
        val logEntry = "[$timestamp] $message\n"
        print(logEntry) // Also print to logcat for real-time debugging
        logBuilder.append(logEntry)
        // Optionally write to file immediately if performance is not an issue
        // appendToFile(logEntry)
    }

    @Synchronized
    fun getLog(): String {
        return logBuilder.toString()
    }

    @Synchronized
    fun clear() {
        logBuilder.clear()
        // Optionally delete the log file
        logFile?.delete()
        logFile = null
        log("--- Log Cleared ---")
    }

    @Synchronized
    fun getLogFileUri(context: Context): Uri? {
        return try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            logDir.mkdirs()
            logFile = File(logDir, "debug_log_${System.currentTimeMillis()}.txt")
            logFile?.writeText(getLog())

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider", // Make sure this matches your FileProvider authority
                logFile!!
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error creating log file URI", e)
            null
        }
    }
}