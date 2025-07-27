package com.bwc.tul

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.bwc.tul.audio.AudioHandler
import com.bwc.tul.ui.dialog.SettingsDialog
import com.bwc.tul.ui.dialog.UserSettingsDialog
import com.bwc.tul.ui.view.MainScreenContent
import com.bwc.tul.viewmodel.MainViewModel
import com.bwc.tul.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            application,
            AudioHandler(
                context = applicationContext,
                onAudioChunk = { viewModel.sendAudio(it) }
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainScreen()
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MainViewModel.ViewEvent.ShowToast -> showToast(event.message)
                    is MainViewModel.ViewEvent.ShowError -> showError(event.message)
                    is MainViewModel.ViewEvent.ShareLogFile -> shareLogFile(event.uri)
                    is MainViewModel.ViewEvent.ExportLogsCompleted -> {
                        showToast("Web Socket Logs Exported")
                    }
                }
            }
        }
    }

    @Composable
    private fun MainScreen() {
        val uiState by viewModel.uiState.collectAsState()
        val prefs = getSharedPreferences("BwctransPrefs", Context.MODE_PRIVATE)

        MainScreenContent(
            onBackClick = { finish() },
            uiState = uiState,
            onMicClick = { viewModel.handleEvent(MainViewModel.UserEvent.MicClicked) },
            onConnectDisconnect = {
                if (uiState.isConnected) {
                    viewModel.handleEvent(MainViewModel.UserEvent.DisconnectClicked)
                } else {
                    viewModel.handleEvent(MainViewModel.UserEvent.ConnectClicked)
                }
            },
            onSettingsClick = { viewModel.handleEvent(MainViewModel.UserEvent.ShowUserSettings) },
            onDevSettingsClick = { viewModel.handleEvent(MainViewModel.UserEvent.ShowDevSettings) }
        )

        if (uiState.showUserSettings) {
            UserSettingsDialog(
                prefs = prefs,
                onDismiss = { viewModel.handleEvent(MainViewModel.UserEvent.DismissDialog) },
                onSave = { apiKey, sourceLang, targetLang ->
                    viewModel.handleEvent(MainViewModel.UserEvent.SettingsSaved(apiKey, sourceLang, targetLang))
                }
            )
        }

        if (uiState.showDevSettings) {
            SettingsDialog(
                onDismissRequest = { viewModel.handleEvent(MainViewModel.UserEvent.DismissDialog) },
                listener = viewModel,
                prefs = prefs,
                models = listOf("gemini-1.5-flash-preview-native-audio-dialog")
            )
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun shareLogFile(uri: android.net.Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share log file"))
    }
}