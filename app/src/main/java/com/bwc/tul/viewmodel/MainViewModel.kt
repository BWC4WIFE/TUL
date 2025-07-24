package com.bwc.tul.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bwc.tul.audio.AudioHandler
import com.bwc.tul.audio.AudioPlayer
import com.bwc.tul.data.ServerResponse
import com.bwc.tul.data.UIState
import com.bwc.tul.websocket.WebSocketClient
import com.bwc.tul.websocket.WebSocketConfig
import com.bwc.tul.ui.view.TranslationItem
import com.bwc.tul.ui.components.Constant
import com.bwc.tul.util.DebugLogger
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(
    application: Application,
    private val audioHandler: AudioHandler
) : AndroidViewModel(application), WebSocketClient.WebSocketListener {

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewEvent>()
    val events = _events.asSharedFlow()

    private var webSocketClient: WebSocketClient? = null
    private val gson = Gson()
    private val logger = DebugLogger
    private var audioPlayer: AudioPlayer? = null
    private var sessionHandle: String? = null

    private val prefs = application.getSharedPreferences(
        "BwctransPrefs", Context.MODE_PRIVATE)

    fun handleEvent(event: UserEvent) {
        viewModelScope.launch {
            when (event) {
                UserEvent.MicClicked -> toggleRecording()
                UserEvent.ConnectClicked -> connectWebSocket()
                UserEvent.DisconnectClicked -> disconnectWebSocket()
                UserEvent.SettingsSaved -> {
                    reloadConfiguration()
                    _events.emit(ViewEvent.ShowToast(
                        "Settings Saved. Please reconnect."))
                }
                UserEvent.RequestPermission ->
                    _events.emit(ViewEvent.ShowUserSettings)
                UserEvent.ShareLogRequested -> handleShareLog()
                UserEvent.ClearLogRequested -> clearDebugLog()
                UserEvent.UserSettingsClicked ->
                    _events.emit(ViewEvent.ShowUserSettings)
                UserEvent.DevSettingsClicked ->
                    _events.emit(ViewEvent.ShowDevSettings)
                UserEvent.ExportLogsCompleted ->  _events.emit(ViewEvent.ShowToast("Web Socket Logs Exported"))

            }
        }
    }

    private fun connectWebSocket() {
        if (_uiState.value.isConnected) {
            logStatus("Already connecting or connected.")
            return
        }
        logStatus("Connecting...")
        audioPlayer?.release()
        audioPlayer = AudioPlayer()
        val config = buildWebSocketConfig()
        webSocketClient = WebSocketClient(config, this, getApplication())
        webSocketClient?.connect()
    }

    private fun disconnectWebSocket() {
        logStatus("Disconnecting...")
        webSocketClient?.disconnect()
        webSocketClient = null
        audioHandler.stopRecording()
        _uiState.update {
            it.copy(
                isRecording = false,
                isListening = false,
                isConnected = false,
                isReady = false,
                statusText = "Disconnected."
            )
        }
    }

    private fun toggleRecording() {
        if (!_uiState.value.isReady) {
            logError("Not ready for audio. Please wait for setup.")
            return
        }
        val newIsListening = !_uiState.value.isListening
        _uiState.update { it.copy(isListening = newIsListening) }

        if (newIsListening) {
            audioHandler.startRecording()
            logStatus("Listening...")
        } else {
            audioHandler.stopRecording()
            logStatus("Recording stopped.")
        }
    }

    fun sendAudio(audioData: ByteArray) {
        webSocketClient?.sendAudio(audioData)
        _uiState.update {
            it.copy(
                isSending = true,
                lastAudioSentTime = System.currentTimeMillis()
            )
        }
    }

    override fun onConnectionOpen() {
        logStatus("Connection open, sending setup...")
        _uiState.update { it.copy(isConnected = true, isReady = false) }
    }

    override fun onSetupComplete() {
        logStatus("Setup complete. Ready to talk.")
        _uiState.update { it.copy(isReady = true) }
        if (!_uiState.value.isListening) {
            toggleRecording()
        }
    }

    override fun onMessage(text: String) {
        logger.log("IN: $text")
        _uiState.update { it.copy(debugLog = logger.getLog()) }
        try {
            val response = gson.fromJson(text, ServerResponse::class.java)

            if (response.goAway != null) {
                logError(
                    "Server sent goAway. Time left: ${response.goAway.timeLeft}")
                disconnectWebSocket()
                return
            }

            response.inputTranscription?.text?.let { t ->
                if (t.isNotBlank()) addOrUpdateTranslation(t, true)
            }
            response.serverContent?.inputTranscription?.text?.let { t ->
                if (t.isNotBlank()) addOrUpdateTranslation(t, true)
            }
            response.outputTranscription?.text?.let { t ->
                if (t.isNotBlank()) addOrUpdateTranslation(t, false)
            }
            response.serverContent?.outputTranscription?.text?.let { t ->
                if (t.isNotBlank()) addOrUpdateTranslation(t, false)
            }
            response.serverContent?.parts?.firstOrNull()?.inlineData?.data?.let {
                audioPlayer?.playAudio(it)
            }
            response.serverContent?.modelTurn?.parts?.firstOrNull()?.inlineData?.data?.let {
                audioPlayer?.playAudio(it)
            }
            response.sessionResumptionUpdate?.let {
                sessionHandle = if (it.resumable == true) it.newHandle else null
                _uiState.update { s ->
                    s.copy(
                        toolbarInfoText = "Session: ${sessionHandle ?: "N/A"}"
                    )
                }
                logStatus("Session handle updated. Resumable: ${it.resumable}")
            }
        } catch (e: Exception) {
            logError("Error parsing message: ${e.message}")
        }
    }

    override fun onClose(reason: String) {
        logStatus("Connection closed: $reason")
        _uiState.update {
            it.copy(
                isConnected = false, isReady = false, isListening = false)
        }
    }

    override fun onError(message: String) {
        logError("WebSocket Error: $message")
        _uiState.update {
            it.copy(
                isConnected = false, isReady = false, isListening = false)
        }
    }

    private fun addOrUpdateTranslation(text: String, isUser: Boolean) {
        _uiState.update { currentState ->
            val newItem = TranslationItem(text = text, isUser = isUser)
            val newTranslations = currentState.translations.toMutableList().apply {
                if (isNotEmpty() && first().isUser == isUser) {
                    set(0, newItem)
                } else {
                    add(0, newItem)
                }
            }
            currentState.copy(translations = newTranslations)
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
            _events.emit(ViewEvent.ShareLogFile(uri))
        } ?: _events.emit(ViewEvent.ShowToast("Log file not available."))
    }

    private fun clearDebugLog() = viewModelScope.launch {
        logger.clear()
        _uiState.update { it.copy(debugLog = "") }
        _events.emit(ViewEvent.ShowToast("On-screen log cleared."))
    }

    private fun logStatus(message: String) {
        Log.i("MainViewModel", message)
        logger.log(message)
        _uiState.update { it.copy(statusText = message, debugLog = logger.getLog()) }
    }

    private fun logError(message: String) {
        Log.e("MainViewModel", message)
        logger.log("ERROR: $message")
        viewModelScope.launch { _events.emit(ViewEvent.ShowError(message)) }
        _uiState.update { it.copy(statusText = message, debugLog = logger.getLog()) }
    }

    private fun buildWebSocketConfig(): WebSocketConfig {
        return WebSocketConfig(
            host = prefs.getString("api_host",
                "generativelanguage.googleapis.com")!!,
            modelName = prefs.getString("selected_model",
                "gemini-2.5-flash-preview-native-audio-dialog")!!,
            vadSilenceMs = prefs.getInt("vad_sensitivity_ms", 800),
            apiVersion = prefs.getString("api_version", "v1alpha")!!,
            apiKey = prefs.getString("api_key", "")!!,
            sessionHandle = sessionHandle,
            systemInstruction = Constant.SYSTEM_INSTRUCTION
        )
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
        object ExportLogsCompleted: ViewEvent() //Added
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
        object ExportLogsCompleted: UserEvent()  //Added
    }
}