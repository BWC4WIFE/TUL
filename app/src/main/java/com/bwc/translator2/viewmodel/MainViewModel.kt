package com.bwc.translator2.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bwc.translator2.R
import com.bwc.translator2.audio.AudioHandler
import com.bwc.translator2.audio.AudioPlayer
import com.bwc.translator2.data.ServerResponse
import com.bwc.translator2.data.UIState
import com.bwc.translator2.network.WebSocketClient
import com.bwc.translator2.ui.components.Constant
import com.bwc.translator2.util.DebugLogger
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.WebSocket
import okio.ByteString
import java.io.File

class MainViewModel(
    application: Application,
    private val audioHandler: AudioHandler
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewEvent>()
    val events = _events.asSharedFlow()

    private var webSocketClient: WebSocketClient? = null
    private val gson = Gson()
    private val logger = DebugLogger
    private var audioPlayer: AudioPlayer? = null
    private var sessionHandle: String? = null

    private val prefs = application.getSharedPreferences("BwctransPrefs", Context.MODE_PRIVATE)

    fun handleEvent(event: UserEvent) {
        viewModelScope.launch {
            when (event) {
                UserEvent.MicClicked -> toggleRecording()
                UserEvent.ConnectClicked -> connectWebSocket()
                UserEvent.DisconnectClicked -> disconnectWebSocket()
                UserEvent.SettingsSaved -> {
                    reloadConfiguration()
                    events.emit(ViewEvent.ShowToast("Settings Saved. Please reconnect."))
                }
                UserEvent.RequestPermission -> events.emit(ViewEvent.ShowUserSettings) // Let activity handle
                UserEvent.ShareLogRequested -> handleShareLog()
                UserEvent.ClearLogRequested -> clearDebugLog()
                UserEvent.UserSettingsClicked -> events.emit(ViewEvent.ShowUserSettings)
                UserEvent.DevSettingsClicked -> events.emit(ViewEvent.ShowDevSettings)
            }
        }
    }

    private fun connectWebSocket() {
        if (_uiState.value.isConnected) {
            logStatus("Already connected.")
            return
        }
        logStatus("Connecting...")

        // Re-initialize audio player for a new session
        audioPlayer?.release()
        audioPlayer = AudioPlayer()

        val config = buildWebSocketConfig()
        // Use the factory to create a new client instance
        webSocketClient = WebSocketClient(
            context = getApplication(),
            config = config,
            listener = createWebSocketListener()
        ).also { client -> // Using a named parameter 'client' is clearer than 'it'
            client.connect()
        }
    }

    private fun disconnectWebSocket() {
        logStatus("Disconnecting...")
        webSocketClient?.disconnect() // This now calls the new public method
        webSocketClient = null
        audioHandler.stopRecording()
        _uiState.update {
            it.copy(
                isRecording = false,
                isListening = false,
                isConnected = false,
                statusText = "Disconnected."
            )
        }
    }

    private fun toggleRecording() {
        if (!_uiState.value.isConnected) {
            logError("Not connected. Cannot start recording.")
            return
        }

        _uiState.update { it.copy(isListening = !it.isListening) }

        if (_uiState.value.isListening) {
            audioHandler.startRecording()
            logStatus("Listening...")
        } else {
            audioHandler.stopRecording()
            logStatus("Recording stopped. Waiting for final translation...")
        }
    }

    fun sendAudio(audioData: ByteArray) {
        if (!_uiState.value.isConnected || !_uiState.value.isListening) return
        webSocketClient?.sendAudio(audioData) // This now calls the new public method
        _uiState.update { it.copy(isSending = true, lastAudioSentTime = System.currentTimeMillis()) }
    }

    private fun handleWebSocketMessage(message: String) {
        try {
            val response = gson.fromJson(message, ServerResponse::class.java)

            // Check for setupComplete message explicitly
            if (message.contains("\"setupComplete\"")) {
                viewModelScope.launch { createWebSocketListener().onSetupComplete() }
            }

            response.inputTranscription?.text?.let { text ->
                if (text.isNotBlank()) addOrUpdateTranslation(text, true)
            }
            response.serverContent?.inputTranscription?.text?.let { text ->
                if (text.isNotBlank()) addOrUpdateTranslation(text, true)
            }

            response.outputTranscription?.text?.let { text ->
                if (text.isNotBlank()) addOrUpdateTranslation(text, false)
            }
            response.serverContent?.outputTranscription?.text?.let { text ->
                if (text.isNotBlank()) addOrUpdateTranslation(text, false)
            }

            response.serverContent?.parts?.firstOrNull()?.inlineData?.data?.let { audioData ->
                audioPlayer?.playAudio(audioData)
            }
            response.serverContent?.modelTurn?.parts?.firstOrNull()?.inlineData?.data?.let { audioData ->
                audioPlayer?.playAudio(audioData)
            }

            response.sessionResumptionUpdate?.let {
                sessionHandle = if(it.resumable == true) it.newHandle else null
                _uiState.update { state -> state.copy(toolbarInfoText = "Session: ${sessionHandle ?: "N/A"}") }
                logStatus("Session handle updated. Resumable: ${it.resumable}")
            }

        } catch (e: Exception) {
            logError("Error parsing message: ${e.message}")
        }
    }

    private fun addOrUpdateTranslation(text: String, isUser: Boolean) {
        _uiState.update { currentState ->
            val translations = currentState.translations.toMutableList()
            if (translations.isNotEmpty() && translations.first().second == isUser) {
                translations[0] = text to isUser
            } else {
                translations.add(0, text to isUser)
            }
            currentState.copy(translations = translations)
        }
    }

    private fun reloadConfiguration() {
        if (_uiState.value.isConnected) {
            disconnectWebSocket()
        }
        logStatus("Configuration reloaded. Please connect again.")
    }

    private fun handleShareLog() = viewModelScope.launch {
        logger.getLogFileUri(getApplication())?.let { uri ->
            events.emit(ViewEvent.ShareLogFile(uri))
        } ?: events.emit(ViewEvent.ShowToast("Log file not available."))
    }

    private fun clearDebugLog() = viewModelScope.launch {
        logger.clear()
        _uiState.update { it.copy(debugLog = "") }
        events.emit(ViewEvent.ShowToast("On-screen log cleared."))
    }

    private fun logStatus(message: String) {
        Log.i("MainViewModel", message)
        logger.log(message)
        _uiState.update { it.copy(statusText = message, debugLog = logger.getLog()) }
    }

    private fun logError(message: String) {
        Log.e("MainViewModel", message)
        logger.log("ERROR: $message")
        viewModelScope.launch { events.emit(ViewEvent.ShowError(message)) }
        _uiState.update { it.copy(statusText = message, debugLog = logger.getLog()) }
    }

    private fun buildWebSocketConfig(): WebSocketClient.WebSocketConfig {
        return WebSocketClient.WebSocketConfig(
            host = prefs.getString("api_host", "generativelanguage.googleapis.com")!!,
            modelName = prefs.getString("selected_model", "gemini-2.5-flash-preview-native-audio-dialog")!!,
            vadSilenceMs = prefs.getInt("vad_sensitivity_ms", 800),
            apiVersion = prefs.getString("api_version", "v1alpha")!!,
            apiKey = prefs.getString("api_key", "")!!,
            sessionHandle = sessionHandle,
            systemInstruction = Constant.SYSTEM_INSTRUCTION
        )
    }

    private fun createWebSocketListener() = object : WebSocketClient.WebSocketListener {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _uiState.update { it.copy(isConnected = true, isListening = false) }
            logStatus("Connected. Sending configuration...")
            // Send configuration message upon connection
            webSocketClient?.sendConfiguration()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            logger.log("IN: $text")
            _uiState.update { it.copy(debugLog = logger.getLog()) }
            handleWebSocketMessage(text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes.utf8())
        }

        override fun onSetupComplete() {
            logStatus("Setup complete. Ready to listen.")
            // Automatically start recording after setup is confirmed
            if (!_uiState.value.isListening) {
                toggleRecording()
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            _uiState.update { it.copy(isConnected = false, isListening = false, statusText = "Connection closing: $reason") }
            logStatus("Connection closing: $code $reason")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _uiState.update { it.copy(isConnected = false, isListening = false) }
            logError("Connection failed: ${t.message}")
            audioHandler.stopRecording()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioHandler.stopRecording()
        audioPlayer?.release()
        webSocketClient?.disconnect()
    }

    sealed class ViewEvent {
        data class ShowToast(val message: String) : ViewEvent()
        data class ShowError(val message: String) : ViewEvent()
        data class ShareLogFile(val uri: Uri) : ViewEvent()
        object ShowUserSettings : ViewEvent()
        object ShowDevSettings : ViewEvent()
    }

    sealed class UserEvent {
        object MicClicked : UserEvent()
        object ConnectClicked : UserEvent()
        object DisconnectClicked : UserEvent()
        object SettingsSaved : UserEvent()
        object RequestPermission : UserEvent()
        object ShareLogRequested : UserEvent()
        object ClearLogRequested : UserEvent()
        object UserSettingsClicked : UserEvent()
        object DevSettingsClicked : UserEvent()
    }
}