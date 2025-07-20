package com.bwc.translator2

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bwc.translator2.audio.AudioHandler
import com.bwc.translator2.ui.dialog.SettingsDialog
import com.bwc.translator2.ui.dialog.UserSettingsDialogFragment
import com.bwc.translator2.ui.view.MainScreen
import com.bwc.translator2.viewmodel.MainViewModel
import com.bwc.translator2.viewmodel.MainViewModelFactory
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity(), UserSettingsDialogFragment.UserSettingsListener, SettingsDialog.DevSettingsListener {

    private lateinit var viewModel: MainViewModel
    private lateinit var audioHandler: AudioHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioHandler = AudioHandler(applicationContext) { audioData ->
            // Pass audio data to ViewModel to send via WebSocket
            // This is handled internally by MainViewModel via audioHandler.startRecording() callback
            // Note: The audio data is now passed directly to the WebSocketClient within MainViewModel
            // This lambda in AudioHandler's constructor is for AudioHandler to provide data,
            // but MainViewModel directly calls webSocketClient.sendAudio(audioData)
            // within its own startRecording method.
        }

        // Fix for "Unresolved reference: Factory" on WebSocketClient.Factory
        // Correctly reference the WebSocketClient.Factory singleton object
        val webSocketClientFactory = WebSocketClient.Companion

        val factory = MainViewModelFactory(application, audioHandler, webSocketClientFactory)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        setContent {
            // Assuming BwcTransTheme is your app's theme
            // BwcTransTheme {
            val uiState by viewModel.uiState.collectAsState()

            MainScreen(viewModel = viewModel)
            // }
        }
    }
    lifecycleScope.launch {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MainViewModel.ViewEvent.ShowToast -> showToast(event.message)
                is MainViewModel.ViewEvent.ShowError -> showError(event.message)
                is MainViewModel.ViewEvent.ShareLogFile -> shareLogFile(event.uri)
                is MainViewModel.ViewEvent.ShowUserSettings -> showUserSettings()
                is MainViewModel.ViewEvent.ShowDevSettings -> showDevSettings()
            }
        }
    }
}

private fun showUserSettings() {
    val userSettingsDialog = UserSettingsDialogFragment()
    userSettingsDialog.show(supportFragmentManager, "UserSettingsDialog")
}

private fun showDevSettings() {
    val models = listOf(
        "gemini-2.5-flash-preview-native-audio-dialog",
        "gemini-2.0-flash-live-001",
        "gemini-2.5-flash-live-preview"
    )
    val prefs = getSharedPreferences("BwctransPrefs", Context.MODE_PRIVATE)
    SettingsDialog(this, this, prefs, models).show()
}

override fun onRequestPermission() {
    viewModel.handleEvent(MainViewModel.UserEvent.RequestPermission)
}

override fun onForceConnect() {
    TODO("Not yet implemented")
}

override fun onSettingsSaved() {
    viewModel.handleEvent(MainViewModel.UserEvent.SettingsSaved)
}

override fun onShareLog() {
    viewModel.handleEvent(MainViewModel.UserEvent.ShareLogRequested)
}

override fun onClearLog() {
    viewModel.handleEvent(MainViewModel.UserEvent.ClearLogRequested)
}

private fun showToast(message: String) {