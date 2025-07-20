package com.bwc.translator2.data

data class UIState(
    val statusText: String = "",
    val sourceText: String = "",
    val translatedText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isReceiving: Boolean = false,
    val shouldAutoTranslate: Boolean = true,
    val isRecording: Boolean = false,
    val isPermissionGranted: Boolean = false
)