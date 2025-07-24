package com.bwc.tul

import android.content.Context
import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.bwc.tul.audio.AudioHandler
import androidx.activity.compose.setContent
import com.bwc.tul.audio.AudioPlayer.Companion.TAG
import com.bwc.tul.ui.dialog.SettingsDialog
import com.bwc.tul.ui.dialog.UserSettingsDialogFragment
import com.bwc.tul.ui.TranslationAdapter
import com.bwc.tul.viewmodel.MainViewModel
import com.bwc.tul.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : FragmentActivity(),
    UserSettingsDialogFragment.UserSettingsListener,
    SettingsDialog.DevSettingsListener {

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
            val uiState by viewModel.uiState.collectAsState()
            MainScreen(viewModel = viewModel)
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is MainViewModel.ViewEvent.ShowToast -> showToast(event.message)
                    is MainViewModel.ViewEvent.ShowError -> showError(event.message)
                    is MainViewModel.ViewEvent.ShareLogFile -> shareLogFile(event.uri)
                    is MainViewModel.ViewEvent.ShowUserSettings -> showUserSettings()
                    is MainViewModel.ViewEvent.ShowDevSettings -> showDevSettings()
                    is MainViewModel.ViewEvent.ExportLogsCompleted -> {
                        // Handle the export logs completed event
                        showToast("Web Socket Logs Exported") }
                    }
            }
        }
    }

    override fun onRequestPermission() {
        viewModel.handleEvent(MainViewModel.UserEvent.RequestPermission)
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

    private fun showUserSettings() {
        val dialog = UserSettingsDialogFragment()
        dialog.show(supportFragmentManager, "UserSettingsDialog")
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

    override fun onForceConnect() {
        viewModel.handleEvent(MainViewModel.UserEvent.ConnectClicked)
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

    override fun onExportLogsComplete(file: File) {
        // Handle the exported log file (e.g., share it).
        viewModel.handleEvent(MainViewModel.UserEvent.ExportLogsCompleted)

        val uri: android.net.Uri? = try {
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.provider", // Make sure this matches your FileProvider authority
                file
            )
        } catch (e: Exception) {
            showError("Error creating log file URI for sharing: ${e.message}")
            Log.e(TAG, "Error creating log file UIR", e)
            null
        }

        uri?.let {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, it)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share WebSocket log file"))
        } ?: run {
            showToast("Couldn't create log file for sharing.")
        }
    }
}