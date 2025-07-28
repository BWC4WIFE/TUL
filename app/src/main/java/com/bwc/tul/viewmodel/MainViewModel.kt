package com.bwc.tul.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bwc.tul.audio.AudioHandler
import com.bwc.tul.audio.AudioPlayer
import com.bwc.tul.data.AppDatabase
import com.bwc.tul.data.ServerResponse
import com.bwc.tul.data.UIState
import com.bwc.tul.ui.components.Constant
import com.bwc.tul.ui.dialog.DevSettingsListener
import com.bwc.tul.ui.view.TranslationItem
import com.bwc.tul.util.DebugLogger
import com.bwc.tul.websocket.WebSocketClient
import com.bwc.tul.websocket.WebSocketConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(
    application: Application,
    private val audioHandler: AudioHandler
) : AndroidViewModel(application), WebSocketClient.WebSocketListener, DevSettingsListener {

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewEvent>()
    val events = _events.asSharedFlow()

    private var webSocketClient: WebSocketClient? = null
    private val gson = Gson()

    private var audioPlayer: AudioPlayer? = null
    private var sessionHandle: String? = null

    private val prefs = application.getSharedPreferences(
        "BwctransPrefs", Context.MODE_PRIVATE)

    init {
        val logDao = AppDatabase.getDatabase(application).logDao()

    }

    override fun onSettingsSaved() {
        _uiState.update { it.copy(showDevSettings = false) }
        reloadConfiguration()
        viewModelScope.launch {
            _events.emit(ViewEvent.ShowToast("Dev Settings Saved. Please reconnect."))
        }
    }

    override fun onShareLog() {
        handleEvent(UserEvent.ShareLogRequested)
    }

    override fun onClearLog() {
        handleEvent(UserEvent.ClearLogRequested)
    }

    override fun onExportLogsComplete(file: File) {
        viewModelScope.launch {
            _events.emit(ViewEvent.ExportLogsCompleted)
        }
    }

    fun handleEvent(event: UserEvent) {
        viewModelScope.launch {
            when (event) {
                UserEvent.MicClicked -> toggleRecording()
                UserEvent.ConnectClicked -> connectWebSocket()
                UserEvent.DisconnectClicked -> disconnectWebSocket()
                is UserEvent.SettingsSaved -> {
                    with(prefs.edit()) {
                        putString("api_key", event.apiKey)
                        putString("source_lang", event.sourceLang)
                        putString("target_lang", event.targetLang)
                        apply()
                    }
                    _uiState.update { it.copy(showUserSettings = false) }
                    reloadConfiguration()
                    _events.emit(ViewEvent.ShowToast("Settings Saved. Please reconnect."))
                }
                UserEvent.ShareLogRequested -> handleShareLog()
                UserEvent.ClearLogRequested -> clearDebugLog()
                UserEvent.ShowUserSettings -> _uiState.update { it.copy(showUserSettings = true) }
                UserEvent.ShowDevSettings -> _uiState.update { it.copy(showDevSettings = true) }
                UserEvent.DismissDialog -> _uiState.update { it.copy(showUserSettings = false, showDevSettings = false) }
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

        viewModelScope.launch {
            val client = try {
                withContext(Dispatchers.IO) {
                    audioPlayer?.release()
                    audioPlayer = AudioPlayer()
                    val config = buildWebSocketConfig()
                    WebSocketClient(config, this@MainViewModel, getApplication())
                }
            } catch (e: Exception) {
                logError("Connection initialization failed: ${e.message}")
                null
            }

            client?.let {
                webSocketClient = it
                it.connect()
            }
        }
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
        // Corrected call to logger.log

        DebugLogger.log("INFO", "WebSocket", "IN: $text")
        // Removed updating the on-screen debug log, as it's now in the database
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
            val newTranslations = listOf(newItem) + currentState.translations
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
        DebugLogger.getLogFileUri(getApplication())?.let { uri ->
            _events.emit(ViewEvent.ShareLogFile(uri))
        } ?: _events.emit(ViewEvent.ShowToast("Log file not available."))
    }

    private suspend fun clearDebugLog() {
        DebugLogger.clear()
        // Removed on-screen log clearing, as it's deprecated
        _uiState.update { it.copy(debugLog = "") }
        _events.emit(ViewEvent.ShowToast("Debug log database cleared."))
    }

    private fun logStatus(message: String) {
        // Corrected call
        DebugLogger.log("INFO", "MainViewModel", message)
        _uiState.update { it.copy(statusText = message) }
    }

    private fun logError(message: String) {
        // Corrected call
        DebugLogger.log("ERROR", "MainViewModel", message)
        viewModelScope.launch { _events.emit(ViewEvent.ShowError(message)) }
        _uiState.update { it.copy(statusText = message) }
    }

    private fun buildWebSocketConfig(): WebSocketConfig {
        return WebSocketConfig(
            host = prefs.getString("api_host",
                "generativelanguage.googleapis.com")!!,
            modelName = prefs.getString("selected_model",
                "gemini-1.5-flash-preview-native-audio-dialog")!!,
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
        object ExportLogsCompleted: ViewEvent()
    }

    sealed class UserEvent {
        object MicClicked : UserEvent()
        object ConnectClicked : UserEvent()
        object DisconnectClicked : UserEvent()
        data class SettingsSaved(val apiKey: String, val sourceLang: String, val targetLang: String) : UserEvent()
        object ShareLogRequested : UserEvent()
        object ClearLogRequested : UserEvent()
        object ShowUserSettings : UserEvent()
        object ShowDevSettings : UserEvent()
        object DismissDialog : UserEvent()
        object ExportLogsCompleted: UserEvent()
    }
}