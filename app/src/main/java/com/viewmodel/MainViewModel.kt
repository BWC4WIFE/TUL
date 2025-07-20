package com.bwc.translator.viewmodel

import com.bwc.translator.ui.components.Constant
import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bwc.translator.R
import com.bwc.translator.audio.AudioHandler
import com.bwc.translator.data.*
import com.bwc.translator.network.WebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider

class MainViewModel(
    private val application: Application,
    private val audioHandler: AudioHandler,
    private val webSocketFactory: WebSocketClient.Companion
) : ViewModel() {

    // State management
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewEvent>()
    val events: SharedFlow<ViewEvent> = _events.asSharedFlow()

    sealed class ViewEvent {
        data class ShowToast(val message: String) : ViewEvent()
        data class ShowError(val message: String) : ViewEvent()
        data class ShareLogFile(val uri: android.net.Uri) : ViewEvent()
    }

    // Configuration
    private val prefs = application.getSharedPreferences("BwctransPrefs", Context.MODE_PRIVATE)
    private val resources = application.resources
    private val models = listOf(
        "gemini-2.5-flash-preview-native-audio-dialog",
        "gemini-2.0-flash-live-001",
        "gemini-2.5-flash-live-preview"
    )

    // Audio/Network
    private var webSocketClient: WebSocketClient? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private val gson = com.google.gson.Gson()

    // Buffers
    private val userInputBuffer = StringBuilder()
    private val modelTranslationBuffer = StringBuilder()
    private var lastSpeakerIsUser: Boolean? = null
    private var sessionHandle: String? = null

    init {
        loadConfiguration()
        updateToolbarText()
    }

    // Public API
    fun handleEvent(event: UserEvent) {
        when (event) {
            UserEvent.MicClicked -> toggleRecording()
            UserEvent.ConnectClicked -> toggleConnection()
            UserEvent.SettingsSaved -> reloadConfiguration()
            UserEvent.RequestPermission -> checkAudioPermission()
            UserEvent.ShareLogRequested -> handleShareLog()
            UserEvent.ClearLogRequested -> clearDebugLog()
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(isMicButtonEnabled = granted) }
    }

    // Core logic
    private fun toggleRecording() {
        if (_uiState.value.isListening) stopRecording() else startRecording()
    }

    private fun toggleConnection() {
        if (_uiState.value.isSessionActive) disconnect() else connect()
    }

    private fun connect() {
        if (_uiState.value.isSessionActive) return

        _uiState.update { it.copy(isSessionActive = true, statusText = "Connecting...") }

        webSocketClient = webSocketFactory.create( // Corrected call to use the factory instance
            context = application, // Pass application context
            config = WebSocketClient.WebSocketConfig(
                host = prefs.getString("api_host", "generativelanguage.googleapis.com") ?: "generativelanguage.googleapis.com",
                modelName = prefs.getString("selected_model", models.first()) ?: models.first(),
                vadSilenceMs = prefs.getInt("vad_sensitivity_ms", 800),
                apiVersion = prefs.getString("api_version", "v1beta") ?: "v1beta",
                apiKey = prefs.getString("api_key", "") ?: "",
                sessionHandle = sessionHandle,
                systemInstruction = Constant.SYSTEM_INSTRUCTION
                //systemInstruction = getString(R.string.system_instruction) // Correctly referencing system_instruction
            ),
            listener = object : WebSocketClient.WebSocketListener {
                override fun onOpen() = handleWebSocketOpen()
                override fun onMessage(text: String) = processServerMessage(text) // 'text' is the parameter, not 'it'
                override fun onClosing(code: Int, reason: String) = handleWebSocketClosing(code, reason)
                override fun onFailure(t: Throwable, response: okhttp3.Response?) = handleWebSocketFailure(t, response)
                override fun onSetupComplete() = handleSetupComplete()
            }
        ).also { it.connect() }
    }

    private fun disconnect() {
        webSocketClient?.disconnect()
        _uiState.update {
            it.copy(
                isSessionActive = false,
                isListening = false,
                statusText = "Disconnected"
            )
        }
        cleanupConnection()
    }

    private fun startRecording() {
        if (!_uiState.value.isSessionActive) return

        audioHandler.startRecording() // Corrected: no lambda needed here, it's set in constructor
        _uiState.update {
            it.copy(
                isListening = true,
                statusText = "Listening..."
            )
        }
    }

    private fun stopRecording() {
        audioHandler.stopRecording()
        _uiState.update {
            it.copy(
                isListening = false,
                statusText = "Ready to listen"
            )
        }
    }

    // WebSocket handlers
    private fun handleWebSocketOpen() {
        reconnectAttempts = 0
        reconnectJob?.cancel()
        _uiState.update {
            it.copy(
                isSessionActive = true,
                statusText = "Connected, configuring server..."
            )
        }
    }

    private fun handleSetupComplete() {
        _uiState.update { it.copy(statusText = "Ready to listen") }
    }

    private fun handleWebSocketClosing(code: Int, reason: String) {
        viewModelScope.launch {
            _events.emit(ViewEvent.ShowToast("Connection closed: $code $reason"))
        }
        scheduleReconnect()
    }

    private fun handleWebSocketFailure(t: Throwable, response: okhttp3.Response?) {
        viewModelScope.launch {
            val message = when (response?.code) {
                404 -> "Server endpoint not found (404)"
                else -> "Connection error: ${t.message}"
            }
            _events.emit(ViewEvent.ShowError(message))
        }
        scheduleReconnect()
    }

    private fun processServerMessage(text: String) {
        try {
            val response = gson.fromJson(text, ServerResponse::class.java)

            response.sessionResumptionUpdate?.let {
                if (it.resumable == true && it.newHandle != null) {
                    sessionHandle = it.newHandle
                    prefs.edit().putString("session_handle", sessionHandle).apply()
                }
            }

            processTranscription(response)
            processAudioOutput(response)
            processTurnCompletion(response)

        } catch (e: Exception) {
            logDebug("Error processing message: ${e.message}")
        }
    }

    private fun processTranscription(response: ServerResponse) {
        // Process user input
        response.inputTranscription?.text?.let { text ->
            if (lastSpeakerIsUser == false) modelTranslationBuffer.clear()
            lastSpeakerIsUser = true
            userInputBuffer.append(text)
            updateTranslation(userInputBuffer.toString().trim(), true)
        }

        // Process model output
        response.outputTranscription?.text?.let { text ->
            if (lastSpeakerIsUser == true) userInputBuffer.clear()
            lastSpeakerIsUser = false
            modelTranslationBuffer.append(text)
            updateTranslation(modelTranslationBuffer.toString().trim(), false)
        }
    }

    private fun processAudioOutput(response: ServerResponse) {
        response.serverContent?.modelTurn?.parts?.forEach { part ->
            part.inlineData?.data?.let { audioData ->
                // Handle audio playback
                logDebug("Received audio response")
            }
        }
    }

    private fun processTurnCompletion(response: ServerResponse) {
        if (response.serverContent?.turnComplete == true) {
            userInputBuffer.clear()
            modelTranslationBuffer.clear()
            lastSpeakerIsUser = null
        }
    }

    // Helpers
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _uiState.update { it.copy(statusText = "Connection failed") }
            reconnectAttempts = 0
            return
        }

        reconnectAttempts++
        val delayMillis = (1000 * Math.pow(2.0, reconnectAttempts.toDouble())).toLong()
        _uiState.update { it.copy(statusText = "Reconnecting in ${delayMillis/1000}s...") }

        reconnectJob = viewModelScope.launch {
            delay(delayMillis)
            connect()
        }
    }

    private fun updateTranslation(text: String, isUser: Boolean) {
        val current = _uiState.value.translations.toMutableList()
        if (lastSpeakerIsUser == isUser && current.isNotEmpty()) {
            current[0] = text to isUser
        } else {
            current.add(0, text to isUser)
        }
        _uiState.update {
            it.copy(
                translations = current,
                showInfoText = current.isEmpty()
            )
        }
    }

    private fun loadConfiguration() {
        sessionHandle = prefs.getString("session_handle", null)
        _uiState.update {
            it.copy(
                showDebugOverlay = prefs.getBoolean("show_debug_overlay", false)
            )
        }
    }

    private fun reloadConfiguration() {
        loadConfiguration()
        updateToolbarText()
        if (_uiState.value.isSessionActive) {
            viewModelScope.launch {
                _events.emit(ViewEvent.ShowToast("Settings updated. Reconnect to apply."))
            }
        }
    }

    private fun updateToolbarText() {
        val model = prefs.getString("selected_model", models.first()) ?: models.first()
        val apiVersion = prefs.getString("api_version", "v1beta") ?: "v1beta"
        _uiState.update {
            it.copy(toolbarInfoText = "Model: $model\nAPI: $apiVersion")
        }
    }

    private fun checkAudioPermission() {
        viewModelScope.launch {
            _events.emit(ViewEvent.ShowToast("Microphone permission required"))
        }
    }

    private fun handleShareLog() {
        viewModelScope.launch {
            webSocketClient?.getLogFile()?.let { file ->
                val uri = FileProvider.getUriForFile(
                    application, // Use 'application' as context
                    "${application.packageName}.provider", // Use 'application' as context
                    file
                )
                _events.emit(ViewEvent.ShareLogFile(uri))
            } ?: _events.emit(ViewEvent.ShowError("No log file available"))
        }
    }

    private fun clearDebugLog() {
        _uiState.update { it.copy(debugLog = "") }
    }

    private fun logDebug(message: String) {
        if (_uiState.value.showDebugOverlay) {
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            _uiState.update {
                it.copy(debugLog = "${it.debugLog}[$timestamp] $message\n")
            }
        }
    }

    private fun cleanupConnection() {
        webSocketClient = null
        reconnectJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
        audioHandler.stopRecording() // Changed to stopRecording as release is private in AudioHandler
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    sealed class UserEvent {
        object MicClicked : UserEvent()
        object ConnectClicked : UserEvent()
        object SettingsSaved : UserEvent()
        object RequestPermission : UserEvent()
        object ShareLogRequested : UserEvent()
        object ClearLogRequested : UserEvent()
    }
}
