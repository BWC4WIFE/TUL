package com.livegemini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import com.bwc.translator.audio.AudioHandler
import com.bwc.translator.network.WebSocketClient
import com.bwc.translator.viewmodel.MainViewModel
import com.bwc.translator.viewmodel.MainViewModelFactory
import com.bwc.translator.ui.view.MainScreen

class MainActivity : ComponentActivity() {

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



    override fun onDestroy() {
        super.onDestroy()
        // ViewModel onCleared() handles audioHandler.release() and websocket disconnect
    }
}
