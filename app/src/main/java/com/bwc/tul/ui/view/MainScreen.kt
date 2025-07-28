 package com.bwc.tul.ui.view

import androidx.compose.ui.Alignment
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
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme
import com.bwc.tul.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    onBackClick: () -> Unit,
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
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp))
                        {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_69_white),
                                contentDescription = "Back",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = onDevSettingsClick,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_bj),
                                contentDescription = "History",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingActionButton(
                    onClick = onMicClick,
                    containerColor = if (uiState.isListening) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_stand_bj),
                        contentDescription = "Mic"
                    )
                }
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Circular connect button on the left
                IconButton(
                    onClick = onConnectDisconnect,
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.CenterStart),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (uiState.isConnected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_69_black),
                        contentDescription = if (uiState.isConnected) "Disconnect" else "Connect",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                StatusBar(
                    statusText = uiState.statusText,
                    toolbarInfoText = uiState.toolbarInfoText,
                    isSessionActive = uiState.isReady,
                    onConnectDisconnect = onConnectDisconnect,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
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

// Preview functions
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
            onDevSettingsClick = {},
            onBackClick = {},
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
            onDevSettingsClick = {},
            onBackClick = {},
        )
    }
}