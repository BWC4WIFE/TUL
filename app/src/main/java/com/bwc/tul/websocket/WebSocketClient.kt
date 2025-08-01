package com.bwc.tul.websocket

import android.util.Log
import com.google.gson.Gson
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class ClientState {
    IDLE, CONNECTING, AWAITING_SETUP_COMPLETE, READY
}
data class WebSocketConfig(    val host: String,
                               val modelName: String,
                               val vadSilenceMs: Int,
                               val apiVersion: String,
                               val apiKey: String,
                               val sessionHandle: String?,
                               val systemInstruction: String
)

class WebSocketClient(
    private val config: WebSocketConfig,
    private val listener: WebSocketListener,
    private val context: Context
) {
    private var webSocket: WebSocket? = null
    private var state: ClientState = ClientState.IDLE
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val webSocketLogger by lazy { WebSocketLogger(context) }

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .addInterceptor(WebSocketInterceptor(context, config.host))
        .build()

    interface WebSocketListener {
        fun onConnectionOpen()
        fun onSetupComplete() // Called when the server confirms setup.
        fun onMessage(text: String) // For all messages after setup
        fun onClose(reason: String)
        fun onError(message: String)
    }

   fun connect() {
        if (state != ClientState.IDLE) {
            Log.w("WebSocketClient", "Already connecting or connected.")
            return
        }
        state = ClientState.CONNECTING
        Log.d("WebSocketClient", "State -> CONNECTING")

        val url = HttpUrl.Builder()
            .scheme("https")
            .host(config.host)
            .addPathSegments("${config.apiVersion}/models/${config.modelName}:generateAnswer")
            .addQueryParameter("key", config.apiKey)
            .build()
			
			scope.launch {
        webSocketLogger.logStatus(url.toString(), "Attempting to connect...")
    }

        val request = Request.Builder().url(url).build()
        webSocket = okHttpClient.newWebSocket(request, SocketListener(url.toString()))
    }

    fun sendAudio(data: ByteArray) {
        if (state != ClientState.READY) {
            Log.w("WebSocketClient", "Not ready to send audio yet.")
            return
        }
        webSocket?.send(ByteString.of(*data))
        webSocket?.request()?.url?.toString()?.let { url ->
            scope.launch {
                webSocketLogger.logSentMessage(url, "[AUDIO DATA of size ${data.size}]")
            }
        }
    }

    fun disconnect() {
        state = ClientState.IDLE
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    private inner class SocketListener(private val url: String) : okhttp3.WebSocketListener() {
        override fun onOpen(ws: WebSocket, response: Response) {
            scope.launch { webSocketLogger.logReceivedMessage(url, "Connection Opened: ${response.code}") }
            listener.onConnectionOpen()
            sendSetupMessage(ws)
        }

        override fun onMessage(ws: WebSocket, text: String) {
            scope.launch { webSocketLogger.logReceivedMessage(url, text) }
            if (state == ClientState.AWAITING_SETUP_COMPLETE) {
                if (text.contains("\"setupComplete\"")) {
                    Log.d("WebSocketClient", "State -> READY")
                    state = ClientState.READY
                    listener.onSetupComplete()
                } else {
                    Log.w("WebSocketClient",
                        "Received unexpected message during setup: $text")
                }
            } else {
                listener.onMessage(text)
            }
        }

        override fun onClosing(ws: WebSocket, code: Int, reason: String) {
            state = ClientState.IDLE
            scope.launch { webSocketLogger.logReceivedMessage(url, "Closing: $code / $reason") }
            listener.onClose(reason)
        }

        override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
            state = ClientState.IDLE
            scope.launch { webSocketLogger.logError(url, "Failure: ${t.message}", t.stackTraceToString()) }
            listener.onError(t.message ?: "Unknown WebSocket error")
        }
    }

    private fun sendSetupMessage(ws: WebSocket) {
        state = ClientState.AWAITING_SETUP_COMPLETE
        Log.d("WebSocketClient", "State -> AWAITING_SETUP_COMPLETE")
        val setupMessage = mapOf("setup" to mutableMapOf<String, Any>().apply {
            put("model", "models/${config.modelName}")
            put("generationConfig", mapOf("responseModalities" to listOf("AUDIO")))
            put("systemInstruction",
                createSystemInstruction(config.systemInstruction))
            put("inputAudioTranscription", emptyMap<String, Any>())
            put("outputAudioTranscription", emptyMap<String, Any>())
            put("contextWindowCompression",
                mapOf("slidingWindow" to emptyMap<String, Any>()))
            put("realtimeInputConfig", mapOf(
                "automaticActivityDetection" to mapOf(
                    "silenceDurationMs" to config.vadSilenceMs)
            ))
            config.sessionHandle?.let { handle ->
                put("sessionResumption", mapOf("handle" to handle))
            }
        })
        val setupJson = gson.toJson(setupMessage)
        Log.d("WebSocketClient", "Sending setup message...")
        ws.send(setupJson)
        scope.launch { webSocketLogger.logSentMessage(ws.request().url.toString(), setupJson) }
    }

    private fun createSystemInstruction(instruction: String): Map<String, Any> {
        return mapOf(
            "parts" to instruction.split(Regex("\n\n+")).map {
                mapOf("text" to it.trim())
            }
        )
    }
}