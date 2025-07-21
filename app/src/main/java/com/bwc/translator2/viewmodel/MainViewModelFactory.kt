package com.bwc.translator2.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bwc.translator2.audio.AudioHandler

class MainViewModelFactory(
    private val application: Application,
    private val audioHandler: AudioHandler,
    ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, audioHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
