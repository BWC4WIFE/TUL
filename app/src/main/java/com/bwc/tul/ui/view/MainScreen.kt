package com.bwc.tul.ui.view

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.tul.data.UIState
import com.bwc.tul.ui.components.StatusBar
import com.bwc.tul.ui.dialog.StatefulSettingsDialog
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme
import com.bwc.tul.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: UIState,
    onMicClick: () -> Unit,
    onConnectDisconnect: () -> Unit,
    onSettingsClick: () -> Unit,
    onDevSettingsClick: () -> Unit
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = uiState.toolbarInfoText,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* handle back */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_69_white),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Fixed icon spacing with proper padding
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = onSettingsClick
                        ){
                          Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        IconButton(
                            onClick = onDevSettingsClick
                        ){

                            Icon(
                                painter = painterResource(id = R.drawable.ic_history),
                                contentDescription = "History",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onMicClick,
                modifier = Modifier.padding(16.dp),
                containerColor = if (uiState.isListening) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stand_bj),
                    contentDescription = "Mic"
                )
            }
        },
        bottomBar = {
            StatusBar(
                statusText = uiState.statusText,
                toolbarInfoText = uiState.toolbarInfoText,
                isSessionActive = uiState.isReady,
                onConnectDisconnect = onConnectDisconnect,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.translations.isEmpty()) {
                Text(
                    text = if (uiState.isReady) "Start speaking..."
                    else "Tap 'Connect' to begin",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.translations) { item ->
                        TranslationItemComposable(item = item)
                    }
                }
            }
        }

        if (uiState.showDebugOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Text(
                    text = uiState.debugLog,
                    color = Color.Green,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    LaunchedEffect(uiState.translations.size) {
        if (uiState.translations.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
}

// Preview functions remain unchanged from your original code
@Preview(showBackground = true, name = "Main Screen - Disconnected Empty")
@Composable
fun MainScreenDisconnectedEmptyPreview() {
    ThaiUncensoredLanguageTheme {
        MainScreenContent(
            uiState = UIState(
                statusText = "Disconnected",
                toolbarInfoText = "Offline",
                isReady = false,
                isListening = false,
                translations = emptyList(),
                showDebugOverlay = false,
                debugLog = ""
            ),
            onMicClick = {},
            onConnectDisconnect = {},
            onSettingsClick = {},
            onDevSettingsClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 120, heightDp = 240, name = "Main Screen - Connected Listening")
@Composable
fun MainScreenConnectedListeningPreview() {
    ThaiUncensoredLanguageTheme {
        MainScreenContent(
            uiState = UIState(
                statusText = "Connected. Listening...",
                toolbarInfoText = "Session active, 1 participant",
                isReady = true,
                isListening = true,
                translations = listOf(
                    TranslationItem(text = "Hello, how are you?", isUser = true),
                    TranslationItem(text = "I'm doing well, thank you!", isUser = false)
                ),
                showDebugOverlay = true,
                debugLog = "Audio processing: ON | Connection: Stable"
            ),
            onMicClick = {},
            onConnectDisconnect = {},
            onSettingsClick = {},
            onDevSettingsClick = {}
        )
    }
}