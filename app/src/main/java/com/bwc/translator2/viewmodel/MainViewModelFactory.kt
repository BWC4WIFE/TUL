package com.bwc.translator2.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bwc.translator2.audio.AudioHandler
import com.bwc.translator2.network.WebSocketClient

class MainViewModelFactory(
    private val application: Application,
    private val audioHandler: AudioHandler,
    private val webSocketFactory: WebSocketClient.Companion // Corrected usage of Factory
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, audioHandler, webSocketFactory) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
