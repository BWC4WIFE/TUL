package com.bwc.tul.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bwc.tul.data.websocket.WebSocketLogger
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun WebSocketLogExporter(
    modifier: Modifier = Modifier,
    onExportComplete: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isExporting = true
            scope.launch {
                val file = WebSocketLogger(context).exportLogs()
                onExportComplete(file)
                isExporting = false
            }
        },
        modifier = modifier,
        enabled = !isExporting
    ) {
        if (isExporting) {
            CircularProgressIndicator(Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(8.dp))
            Text("Exporting...")
        } else {
            Text("Export WebSocket Logs")
        }
    }
}