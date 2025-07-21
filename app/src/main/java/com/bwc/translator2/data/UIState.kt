package com.bwc.translator2.data

import com.bwc.translator2.ui.view.TranslationItem

data class UIState(
    val statusText: String = "",
    val toolbarInfoText: String = "",
    val isListening: Boolean = false,
    val translations: List<TranslationItem> = emptyList(),
    val showDebugOverlay: Boolean = false,
    val debugLog: String = "",
    val isRecording: Boolean = false,
    val isConnected: Boolean = false,
    val isSending: Boolean = false,
    val isReady: Boolean = false,
    val lastAudioSentTime: Long = 0
)

