package com.bwc.translator2.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bwc.translator2.R
import com.bwc.translator2.data.UIState // Assuming UIState is defined in this package or imported correctly
import com.bwc.translator2.ui.components.StatusBar // CORRECTED: Proper import for StatusBar
import com.bwc.translator2.ui.theme.ThaiUncensoredLanguageTheme
import com.bwc.translator2.viewmodel.MainViewModel


// ===================================================================================
// 1. STATEFUL Composable: Connects to ViewModel and delegates to stateless content
// ===================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    // Collect UI State from ViewModel
    val uiState by viewModel.uiState.collectAsState()

    // Pass collected state and event handlers to the stateless content composable
    MainScreenContent(
        uiState = uiState,
        onMicClick = { viewModel.handleEvent(MainViewModel.UserEvent.MicClicked) },
        onConnectDisconnect = {
                    if (uiState.isConnected) {
                        viewModel.handleEvent(MainViewModel.UserEvent.DisconnectClicked)
                    } else {
                        viewModel.handleEvent(MainViewModel.UserEvent.ConnectClicked)
                    }
                },
        onSettingsClick = { viewModel.handleEvent(MainViewModel.UserEvent.UserSettingsClicked) },
        onBackClick = { /* Handle back navigation or ViewModel event */ }
        // Note: History icon click is not handled by ViewModel event here, consider adding it if needed.
    )
}

// ===================================================================================
// 2. STATELESS Composable: Receives all necessary data and callbacks as parameters
//    Does NOT know about ViewModel directly. Easily previewable.
// ===================================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: UIState,
    onMicClick: () -> Unit,
    onConnectDisconnect: () -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit,
    // Add other event handlers as needed, e.g., onHistoryClick: () -> Unit
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(text = uiState.toolbarInfoText, style = MaterialTheme.typography.labelSmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { /* Handle history click if needed, pass as parameter */ }) {
                        // Using a placeholder icon, ensure you have one or pass it
                        Icon(painter = painterResource(id = R.drawable.ic_history),
                            contentDescription = "History") // CONSIDER: Using a more appropriate history icon if available, or just keeping the placeholder if Phone is intended for something else.
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onMicClick,
                modifier = Modifier.padding(16.dp),
                containerColor = if (uiState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_bj), contentDescription = "Mic")
            }
        },
        bottomBar = {
            // CORRECTED: Use the dedicated StatusBar composable
            StatusBar(
                statusText = uiState.statusText,
                toolbarInfoText = uiState.toolbarInfoText,
                isSessionActive = uiState.isReady ,
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
                    // CORRECTED: More appropriate text for empty state when not listening
                    text = if (uiState.isReady) "Start speaking..." else "Tap 'Connect' to begin",
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
                        TranslationItemComposable(item = TranslationItem(
                            text = item.text,
                            isUser = item.isUser)
                        )


                    }
                }
            }
        }

        // Debug overlay remains as is
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
                    fontFamily = FontFamily.Monospace, // Using Compose's FontFamily
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    // Scroll to bottom when new item is added
    LaunchedEffect(uiState.translations.size) {
        if (uiState.translations.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
}


// ===================================================================================
// 3. PREVIEWS: Only for the stateless MainScreenContent
// ===================================================================================

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
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Main Screen - Connected Listening")
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
                    TranslationItem(
                        text = "Hello, how are you?",
                        isUser = true),
                    TranslationItem(
                        text = "I'm doing well, thank you!",
                        isUser = false)
                ),
                showDebugOverlay = true,
                debugLog = "Audio processing: ON | Connection: Stable"
            ),
            onMicClick = {},
            onConnectDisconnect = {},
            onSettingsClick = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Main Screen - Connected Not Listening")
@Composable
fun MainScreenConnectedNotListeningPreview() {
    ThaiUncensoredLanguageTheme {
        MainScreenContent(
            uiState = UIState(
                statusText = "Connected. Ready to listen.",
                toolbarInfoText = "Session active, 1 participant",
                isReady = true,
                isListening = false,
                isConnected = true,
                translations = listOf(
                    TranslationItem(
                        text = "Hello, how are you?",
                        isUser = true
                    ),
                    TranslationItem(
                        text = "สวัสดี คุณเป็นอย่างไรบ้าง",
                        isUser = false
                    )
                ),
                showDebugOverlay = false,
                debugLog = ""
            ),
            onMicClick = {},
            onConnectDisconnect = {},
            onSettingsClick = {},
            onBackClick = {}
        )
    }
}