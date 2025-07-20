package com.bwc.translator.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
                .padding(8.dp)
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
