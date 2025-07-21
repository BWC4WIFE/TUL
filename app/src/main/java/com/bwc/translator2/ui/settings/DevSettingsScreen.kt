package com.bwc.translator2.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.translator2.ui.theme.ThaiUncensoredLanguageTheme

/**
 * A stateless composable for displaying developer settings.
 * State is hoisted to the caller.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSettingsContent(
    apiHost: String,
    onApiHostChange: (String) -> Unit,
    apiVersion: String,
    onApiVersionChange: (String) -> Unit,
    vadSensitivity: String,
    onVadSensitivityChange: (String) -> Unit,
    availableModels: List<String>,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onSave: () -> Unit,
    onShareLog: () -> Unit,
    onClearLog: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Dev Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // Model Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedModel,
                    onValueChange = {},
                    label = { Text("Selected Model") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                onModelSelected(model)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiHost,
                onValueChange = onApiHostChange,
                label = { Text("API Host") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiVersion,
                onValueChange = onApiVersionChange,
                label = { Text("API Version") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = vadSensitivity,
                onValueChange = onVadSensitivityChange,
                label = { Text("VAD Sensitivity (ms)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row {
                Button(onClick = onShareLog) { Text("Share Log") }
                Spacer(Modifier.weight(1f))
                Button(onClick = onClearLog) { Text("Clear Log") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("Save Settings")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DevSettingsContentPreview() {
    ThaiUncensoredLanguageTheme {
        DevSettingsContent(
            apiHost = "generativelanguage.googleapis.com",
            onApiHostChange = {},
            apiVersion = "v1alpha",
            onApiVersionChange = {},
            vadSensitivity = "800",
            onVadSensitivityChange = {},
            availableModels = listOf("model-1", "model-2"),
            selectedModel = "model-1",
            onModelSelected = {},
            onSave = {},
            onShareLog = {},
            onClearLog = {}
        )
    }
}