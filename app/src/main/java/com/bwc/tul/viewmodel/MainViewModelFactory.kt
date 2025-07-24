package com.bwc.tul.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bwc.tul.audio.AudioHandler

class MainViewModelFactory(
    private val application: Application,
    private val audioHandler: AudioHandler,
) : ViewModelProvider.Factory { // line 7
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, audioHandler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}