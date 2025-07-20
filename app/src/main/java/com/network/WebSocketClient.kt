package com.bwc.translator.network

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okio.ByteString
import java.io.File
import okhttp3.logging.HttpLoggingInterceptor
import java.io.FileWriter
import java.io.PrintWriter
import java.util.concurrent.TimeUnit

class WebSocketClient private constructor(
    private val context: Context,
    private val config: WebSocketConfig,
    private val listener: WebSocketListener
) {
    companion object {
        private const val TAG = "WebSocketClient"

        // Modified: Moved the 'create' function directly into the companion object
        fun create(
            context: Context,
            config: WebSocketConfig,
            listener: WebSocketListener
        ): WebSocketClient {
            return WebSocketClient(context, config, listener)
        }
        private const val SYS = "System_Instruction"
    }

    private var webSocket: WebSocket? = null
    private var isSetupComplete = false
    private var isConnected = false
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

    fun sendAudio(audioData: ByteArray) {
        if (!isReady()) return

        scope.launch {
            try {
                val message = createAudioMessage(audioData)
                logMessage("OUTGOING AUDIO", "length=${audioData.size}")
                webSocket?.send(message)
            } catch (e: Exception) {
                logError("Failed to send audio", e)
            }
        }
    }

    fun disconnect() {
        scope.launch {
            cleanupResources()
        }
    }

    fun getLogFile(): File? = logFile

    fun isReady(): Boolean = isConnected && isSetupComplete

    private fun initializeLogging() {
        try {
            val logDir = File(context.getExternalFilesDir(null), "websocket_logs").apply {
                mkdirs()
            }
            logFile = File(logDir, "session_${System.currentTimeMillis()}.log").apply {
                logFileWriter = PrintWriter(FileWriter(this, true), true)
                logFileWriter?.println("--- Session Started ${java.util.Date()} ---")
            }
        } catch (e: Exception) {
            logError("Failed to initialize logging", e)
            listener.onFailure(e, null)
        }
    }

    private fun initializeWebSocket() {
        val request = Request.Builder()
            .url(buildWebSocketUrl())
            .build()

        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                handleWebSocketOpen(response)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleTextMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                handleBinaryMessage(bytes)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                handleWebSocketClosing(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleWebSocketFailure(t, response)
            }
        })
    }

    private fun handleWebSocketOpen(response: Response) {
        scope.launch {
            logMessage("CONNECTED", "HTTP ${response.code}")
            isConnected = true
            sendConfiguration()
            listener.onOpen()
        }
    }

    private fun handleTextMessage(text: String) {
        scope.launch {
            logMessage("INCOMING TEXT", text.take(300))
            processMessage(text)
        }
    }

    private fun handleBinaryMessage(bytes: ByteString) {
        scope.launch {
            logMessage("INCOMING BINARY", "size=${(bytes.size)}")
            processMessage(bytes.utf8())
        }
    }

    private fun handleWebSocketClosing(code: Int, reason: String) {
        scope.launch {
            logMessage("CLOSING", "code=$code reason=$reason")
            cleanupResources()
            listener.onClosing(code, reason)
        }
    }

    private fun handleWebSocketFailure(t: Throwable, response: Response?) {
        scope.launch {
            logError("FAILURE", t)
            cleanupResources()
            listener.onFailure(t, response)
        }
    }

    private fun processMessage(message: String) {
        try {
            if (message.contains("\"setupComplete\"")) {
                isSetupComplete = true
                listener.onSetupComplete()
            }
            listener.onMessage(message)
        } catch (e: Exception) {
            logError("Message processing failed", e)
        }
    }

    private fun sendConfiguration() {
        val configMessage = gson.toJson(config.createSetupMessage())
        logMessage("CONFIG SENT", configMessage.take(300))
        webSocket?.send(configMessage)
    }

    private fun cleanupResources() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        logFileWriter?.apply {
            println("--- Session Ended ${java.util.Date()} ---")
            flush()
            close()
        }
        isConnected = false
        isSetupComplete = false
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

    interface WebSocketListener {
        fun onOpen()
        fun onMessage(text: String)
        fun onClosing(code: Int, reason: String)
        fun onFailure(t: Throwable, response: Response?)
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
}
