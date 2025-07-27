package com.bwc.tul.ui.screens

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import java.io.File
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bwc.tul.R
import com.bwc.tul.websocket.*
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLogScreen(
    logs: List<String>,
    onNavigateBack: () -> Unit,
    onExportLogs: (File) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Logs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_69_white),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logs.size) { index ->
                Card {
                    Text(
                        text = logs[index],
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun DebugLogScreenPreview() {
    ThaiUncensoredLanguageTheme {
        DebugLogScreen(
            logs = listOf(
                "[12:34:56] Connected to server",
                "[12:35:01] Received audio data (size: 1024 bytes)",
                "[12:35:02] Sent translation request",
                "[12:35:03] Received translation response",
                "[12:35:05] Disconnected from server"
            ),
            onNavigateBack = {},
            onExportLogs = {}
        )
    }
}
