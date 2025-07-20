package com.bwc.translator2.viewmodel

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bwc.translator2.audio.AudioHandler
import com.bwc.translator2.data.UIState
import com.bwc.translator2.network.WebSocketClient
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import java.util.concurrent.TimeUnit

class MainViewModel(
    application: Application,
    private val audioHandler: AudioHandler,
    private val webSocketFactory: WebSocketClient.Companion
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewEvent>()
    val events = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var currentSessionId: String = generateSessionId()

    fun sendAudio(audioData: ByteArray) {
        viewModelScope.launch {
            try {
                val base64Audio = Base64.encodeToString(audioData, Base64.NO_WRAP)
                val messagePayload = mapOf(
                    "audio_data" to base64Audio,
                    "timestamp" to System.currentTimeMillis(),
                    "session_id" to currentSessionId
                )
                val jsonMessage = Gson().toJson(messagePayload)
                webSocket?.send(jsonMessage)

                _uiState.update { currentState ->
                    currentState.copy(
                        isSending = true,
                        lastAudioSentTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _events.emit(ViewEvent.ShowError("Failed to send audio: ${e.message}"))
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun connectWebSocket() {
        viewModelScope.launch {
            try {
                val client = OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build()

                val request = Request.Builder()
                    .url("wss://your-websocket-endpoint.com")
                    .build()

                webSocket = client.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        _uiState.update { it.copy(isConnected = true) }
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        handleWebSocketMessage(text)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        _uiState.update { it.copy(isConnected = false) }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        _events.emit(ViewEvent.ShowError("WebSocket error: ${t.message}"))
                        _uiState.update { it.copy(isConnected = false) }
                    }
                })
            } catch (e: Exception) {
                _events.emit(ViewEvent.ShowError("Connection failed: ${e.message}"))
            }
        }
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}"
    }

    private fun handleWebSocketMessage(message: String) {
        // Implement message handling logic
    }

    override fun onCleared() {
        super.onCleared()
        webSocket?.close(1000, "Activity destroyed")
        audioHandler.stopRecording()
    }

    sealed class ViewEvent {
        data class ShowToast(val message: String) : ViewEvent()
        data class ShowError(val message: String) : ViewEvent()
        data class ShareLogFile(val uri: android.net.Uri) : ViewEvent()
        object ShowUserSettings : ViewEvent()
        object ShowDevSettings : ViewEvent()
    }

    sealed class UserEvent {
        object MicClicked : UserEvent()
        object ConnectClicked : UserEvent()
        object SettingsSaved : UserEvent()
        object RequestPermission : UserEvent()
        object ShareLogRequested : UserEvent()
        object ClearLogRequested : UserEvent()
        object UserSettingsClicked : UserEvent()
        object DevSettingsClicked : UserEvent()
    }
}