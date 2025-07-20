package com.bwc.translator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bwc.translator2.audio.AudioHandler
import com.bwc.translator2.util.DebugLog
import com.bwc.translator2.data.UiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

class MainViewModel(
    private val audioHandler: AudioHandler,
    private val debugLog: DebugLog,
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ViewEvent>()
    val events = _events.asSharedFlow()

    fun handleEvent(event: UserEvent) {
        when (event) {
            UserEvent.MicClicked -> toggleRecording()
            UserEvent.ConnectClicked -> toggleConnection()
            UserEvent.SettingsSaved -> reloadConfiguration()
            UserEvent.RequestPermission -> checkAudioPermission()
            UserEvent.ShareLogRequested -> handleShareLog()
            UserEvent.ClearLogRequested -> clearDebugLog()
            UserEvent.UserSettingsClicked -> viewModelScope.launch { _events.emit(ViewEvent.ShowUserSettings) }
            UserEvent.DevSettingsClicked -> viewModelScope.launch { _events.emit(ViewEvent.ShowDevSettings) }
        }
    }

    private fun handleShareLog() {
        val logFile = debugLog.getLogFile(getApplication<Application>().applicationContext)
        if (logFile != null) {
            viewModelScope.launch {
                _events.emit(ViewEvent.ShareLogFile(logFile))
            }
        } else {
            viewModelScope.launch {
                _events.emit(ViewEvent.ShowToast("Log file not found."))
            }
        }
    }

    private fun clearDebugLog() {
        debugLog.clearLog(getApplication<Application>().applicationContext)
        viewModelScope.launch { _events.emit(ViewEvent.ShowToast("Debug log cleared.")) }
    }

    private fun toggleRecording() {
        if (_uiState.value.isRecording) {
            audioHandler.stopRecording()
            _uiState.update { it.copy(isRecording = false) }
        } else {
            // Check for permission before starting to record.
            if (audioHandler.hasRecordAudioPermission(getApplication())) {
                audioHandler.startRecording()
                _uiState.update { it.copy(isRecording = true) }
            } else {
                // If permission is missing, signal the UI to ask for it.
                viewModelScope.launch { _events.emit(ViewEvent.ShowError("Microphone permission is required to record.")) }
            }
        }
    }

    private fun toggleConnection() {
        // This assumes the AudioHandler manages the connection lifecycle.
        if (_uiState.value.isConnected) {
            audioHandler.stop()
            _uiState.update { it.copy(isConnected = false) }
        } else {
            audioHandler.start()
            _uiState.update { it.copy(isConnected = true) }
        }
    }

    private fun reloadConfiguration() {
        // This assumes the AudioHandler can dynamically reload its configuration.
        viewModelScope.launch {
            audioHandler.reloadConfig()
            _events.emit(ViewEvent.ShowToast("Configuration reloaded."))
        }
    }

    private fun checkAudioPermission() {
        // This function is for explicitly checking permission, e.g., from a button.
        if (audioHandler.hasRecordAudioPermission(getApplication())) {
            viewModelScope.launch {
                _events.emit(ViewEvent.ShowToast("Microphone permission is already granted."))
            }
        } else {
            // Signal the UI that permission needs to be requested.
            // The Activity should observe this and trigger the system's permission dialog.
            viewModelScope.launch {
                _events.emit(ViewEvent.ShowError("Please grant microphone permission via the system dialog."))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioHandler.shutdown()
    }
}
