package com.bwc.tul.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme

/**
 * A stateless component to display connection status and provide a connect/disconnect action.
 */
@Composable
fun StatusBar(
    statusText: String,
    toolbarInfoText: String,
    isSessionActive: Boolean,
    onConnectDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp) // Consistent padding
        ) {
            Text(
                text = statusText,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start
            )
            Button(
                onClick = onConnectDisconnect,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(if (isSessionActive) "Disconnect" else "Connect")
            }
        }
        Text(
            text = toolbarInfoText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true, name = "Status Bar - Disconnected")
@Composable
fun StatusBarDisconnectedPreview() {
    ThaiUncensoredLanguageTheme {
        StatusBar(
            statusText = "Disconnected. Tap to connect.",
            toolbarInfoText = "Session: N/A",
            isSessionActive = false,
            onConnectDisconnect = {}
        )
    }
}

@Preview(showBackground = true, name = "Status Bar - Connected")
@Composable
fun StatusBarConnectedPreview() {
    ThaiUncensoredLanguageTheme {
        StatusBar(
            statusText = "Connected. Listening...",
            toolbarInfoText = "Session: active-session-123",
            isSessionActive = true,
            onConnectDisconnect = {}
        )
    }
}