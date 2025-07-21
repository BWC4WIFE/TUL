package com.bwc.translator2.ui.view

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bwc.translator2.viewmodel.MainViewModel
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp // Added import
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Phone // Added import
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.bwc.translator2.R
import com.bwc.translator2.data.UIState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import com.bwc.translator2.ui.view.TranslationItemComposable // Ensured import is present
import com.bwc.translator2.ui.view.TranslationItem // Ensured import is present


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState
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
                    IconButton(onClick = { /* Handle back navigation */ }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.handleEvent(MainViewModel.UserEvent.UserSettingsClicked) }) {
                        Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Settings")
                    }
                    IconButton(onClick = { /* Handle history click */ }) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = "History")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.handleEvent(MainViewModel.UserEvent.MicClicked) },
                modifier = Modifier.padding(16.dp),
                containerColor = if (uiState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) {
                  Icon(Icons.Filled.ThumbUp, contentDescription = "Mic")
            //    Icon(painter = painterResource(id = R.drawable.ic_mic), contentDescription = "Mic")
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = uiState.statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when {
                        uiState.statusText.contains("error", ignoreCase = true) -> Color.Red
                        uiState.isListening -> Color.Green
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
        ) {
            if (uiState.translations.isEmpty()) {
                Text(
                    text = uiState.statusText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.translations) { (text, isUser) ->
                        TranslationItemComposable(item = TranslationItem(text = text, isUser = isUser))
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
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
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
