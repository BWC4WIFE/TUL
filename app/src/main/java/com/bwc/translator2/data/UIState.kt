// app/src/main/java/com.bwc.translator2.data/UiState.kt
package com.bwc.translator2.data

data class UiState(
    val statusText: String = "Status: Disconnected\nTap the microphone to connect",
    val toolbarInfoText: String = "Model: N/A\nAPI: N/A",
    val isListening: Boolean = false,
    val isSessionActive: Boolean = false,
    val connectButtonText: String = "Connect",
    val translations: List<Pair<String, Boolean>> = emptyList(),
    val showInfoText: Boolean = true,
    val isMicButtonEnabled: Boolean = false,
    val showDebugOverlay: Boolean = false,
    val debugLog: String = ""
)
