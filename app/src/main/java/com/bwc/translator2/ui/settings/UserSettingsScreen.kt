package com.bwc.translator2.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.translator2.ui.theme.ThaiUncensoredLanguageTheme

/**
 * A stateless composable for displaying user settings.
 * State is hoisted to the caller.
 */
@Composable
fun UserSettingsContent(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    sourceLang: String,
    onSourceLangChange: (String) -> Unit,
    targetLang: String,
    onTargetLangChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(modifier = Modifier.padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("User Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = sourceLang,
                onValueChange = onSourceLangChange,
                label = { Text("Source Language (e.g., en-US)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = targetLang,
                onValueChange = onTargetLangChange,
                label = { Text("Target Language (e.g., es-ES)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Save and Dismiss")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UserSettingsContentPreview() {
    ThaiUncensoredLanguageTheme {
        UserSettingsContent(
            apiKey = "preview_api_key",
            onApiKeyChange = {},
            sourceLang = "en-US",
            onSourceLangChange = {},
            targetLang = "es-ES",
            onTargetLangChange = {},
            onSave = {},
            onDismiss = {}
        )
    }
}