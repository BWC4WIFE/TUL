package com.bwc.translator2.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.concurrent.TimeUnit

class WebSocketClient(
    private val context: Context,
    private val config: WebSocketConfig,
    private val listener: WebSocketListener
) {
    private var webSocket: WebSocket? = null
    private var isSetupComplete = false
    @Volatile private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private val gson = Gson()
    private var logFileWriter: PrintWriter? = null
    private var logFile: File? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .addInterceptor(createLoggingInterceptor())
        .build()

    fun connect() {
        if (isConnected) return
        initializeLogging()
        initializeWebSocket()
    }

    fun disconnect() {
        if (!isConnected) return
        scope.launch {
            webSocket?.close(1001, "Client disconnected")
            cleanupResources()
        }
    }

    fun sendAudio(audioData: ByteArray) {
        if (!isConnected || !isSetupComplete) {
            logMessage("WARN", "Connection not ready, audio data dropped.")
            return
        }
        val audioMessage = createAudioMessage(audioData)
        webSocket?.send(audioMessage)
        logMessage("OUTGOING AUDIO", "size=${audioData.size}")
    }

    private fun initializeLogging() {
        try {
            val logDir = File(context.getExternalFilesDir(null), "websocket_logs")
            logDir.mkdirs()
            logFile = File(logDir, "session_${System.currentTimeMillis()}.log").apply {
                logFileWriter = PrintWriter(FileWriter(this, true), true)
                logFileWriter?.println("--- Session Started ${java.util.Date()} ---")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize logging", e)
            listener.onFailure(null, e, null)
        }
    }

    private fun initializeWebSocket() {
        val request = Request.Builder()
            .url(buildWebSocketUrl())
            .build()

        webSocket = client.newWebSocket(request, this.listener)
    }

    private fun cleanupResources() {
        if (!isConnected) return
        isConnected = false
        isSetupComplete = false
        webSocket = null
        logFileWriter?.apply {
            println("--- Session Ended ${java.util.Date()} ---")
            flush()
            close()
        }
        logFileWriter = null
        logFile = null
    }

    private fun createAudioMessage(audioData: ByteArray): String {
        return gson.toJson(mapOf(
            "realtimeInput" to mapOf(
                "audio" to mapOf(
                    "data" to Base64.encodeToString(audioData, Base64.NO_WRAP),
                    "mime_type" to "audio/pcm;rate=16000"
                )
            )
        ))
    }

    private fun processMessage(message: String) {
        try {
            if (message.contains("\"setupComplete\"")) {
                isSetupComplete = true
                (listener as? WebSocketListener)?.onSetupComplete()
            }
            listener.onMessage(null, message)
        } catch (e: Exception) {
            logError("Message processing failed", e)
        }
    }

    fun sendConfiguration() {
        val configMessage = gson.toJson(config.createSetupMessage())
        logMessage("CONFIG SENT", configMessage.take(300))
        webSocket?.send(configMessage)
        isSetupComplete = true
    }

    private fun buildWebSocketUrl(): String {
        return "wss://${config.host}/ws/google.ai.generativelanguage.${config.apiVersion}" +
                ".GenerativeService.BidiGenerateContent?key=${config.apiKey}"
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor { message ->
            Log.d(TAG, message)
            logFileWriter?.println("NETWORK: $message")
        }.apply { level = HttpLoggingInterceptor.Level.BODY }
    }

    private fun logMessage(tag: String, message: String) {
        Log.d(TAG, "$tag: $message")
        logFileWriter?.println("$tag: $message")
    }

    private fun logError(context: String, error: Throwable) {
        Log.e(TAG, context, error)
        logFileWriter?.println("ERROR [$context]: ${error.message}")
        error.printStackTrace(logFileWriter)
    }

    interface WebSocketListener : okhttp3.WebSocketListener {
        fun onSetupComplete()
    }

    data class WebSocketConfig(
        val host: String,
        val modelName: String,
        val vadSilenceMs: Int,
        val apiVersion: String,
        val apiKey: String,
        val sessionHandle: String?,
        val systemInstruction: String
    ) {
        fun createSetupMessage(): Map<String, Any> {
            return mapOf("setup" to mutableMapOf<String, Any>().apply {
                put("model", "models/$modelName")
                put("generationConfig", mapOf("responseModalities" to listOf("AUDIO")))
                put("systemInstruction", createSystemInstruction())
                put("inputAudioTranscription", emptyMap<String, Any>())
                put("outputAudioTranscription", emptyMap<String, Any>())
                put("contextWindowCompression", mapOf("slidingWindow" to emptyMap<String, Any>()))
                put("realtimeInputConfig", mapOf(
                    "automaticActivityDetection" to mapOf("silenceDurationMs" to vadSilenceMs)
                ))
                sessionHandle?.let {
                    put("sessionResumption", mapOf("handle" to it))
                }
            })
        }

        private fun createSystemInstruction(): Map<String, Any> {
            return mapOf("parts" to systemInstruction.split(Regex("\n\n+")).map {
                mapOf("text" to it.trim())
            })
        }
    }

    companion object {
        private const val TAG = "WebSocketClient"
    }
}