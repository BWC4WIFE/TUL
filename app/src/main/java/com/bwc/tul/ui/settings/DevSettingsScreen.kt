package com.bwc.tul.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bwc.tul.data.ApiVersion
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme
import kotlin.math.roundToInt
import com.bwc.tul.ui.components.WebSocketLogExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevSettingsContent(
    apiHost: String,
    onApiHostChange: (String) -> Unit,
    availableApiVersions: List<ApiVersion>,
    selectedApiVersion: String,
    onApiVersionChange: (String) -> Unit,
    vadSensitivity: String,
    onVadSensitivityChange: (String) -> Unit,
    availableModels: List<String>,
    selectedModel: String,
    onModelSelected: (String) -> Unit,
    onSave: () -> Unit,
    onShareLog: () -> Unit,
    onClearLog: () -> Unit,
    onExportLogsComplete: (java.io.File) -> Unit = {}
) {
    var modelDropdownExpanded by remember { mutableStateOf(false) }

    // Map API versions to slider positions (0-3)
    val apiVersionPositions = availableApiVersions.mapIndexed { index, _ -> index }
    val currentApiVersionIndex = availableApiVersions.indexOfFirst { it.value == selectedApiVersion }
        .coerceIn(0, availableApiVersions.size - 1)

    Surface(modifier = Modifier.padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Dev Settings", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            // Model Dropdown
            ExposedDropdownMenuBox(
                expanded = modelDropdownExpanded,
                onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedModel,
                    onValueChange = {},
                    label = { Text("Selected Model") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelDropdownExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = modelDropdownExpanded,
                    onDismissRequest = { modelDropdownExpanded = false }
                ) {
                    availableModels.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                onModelSelected(model)
                                modelDropdownExpanded = false
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
            Spacer(modifier = Modifier.height(16.dp))

            // VAD Sensitivity Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "VAD Sensitivity: ${vadSensitivity}ms",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = vadSensitivity.toFloatOrNull() ?: 800f,
                    onValueChange = {
                        onVadSensitivityChange(it.roundToInt().toString())
                    },
                    valueRange = 100f..2000f,
                    steps = 37
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // API Version Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "API Version: ${availableApiVersions[currentApiVersionIndex].displayName}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = currentApiVersionIndex.toFloat(),
                    onValueChange = { newValue ->
                        val index = newValue.roundToInt().coerceIn(0, availableApiVersions.size - 1)
                        onApiVersionChange(availableApiVersions[index].value)
                    },
                    valueRange = 0f..(availableApiVersions.size - 1).toFloat(),
                    steps = availableApiVersions.size - 2
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    availableApiVersions.forEach { version ->
                        Text(
                            text = version.displayName.take(6), // Show abbreviated names
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

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
            Spacer(modifier = Modifier.height(8.dp))
            WebSocketLogExporter(onExportComplete =  onExportLogsComplete )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DevSettingsContentPreview() {
    val sampleApiVersions = listOf(
        ApiVersion("v1alpha (Stable)", "v1alpha"),
        ApiVersion("v1beta (Preview)", "v1beta"),
        ApiVersion("v1beta1 (Experimental)", "v1beta1"),
        ApiVersion("v1 (Latest)", "v1")
    )
    ThaiUncensoredLanguageTheme {
        DevSettingsContent(
            apiHost = "generativelanguage.googleapis.com",
            onApiHostChange = {},
            availableApiVersions = sampleApiVersions,
            selectedApiVersion = "v1beta",
            onApiVersionChange = {},
            vadSensitivity = "1250",
            onVadSensitivityChange = {},
            availableModels = listOf("model-A", "model-B"),
            selectedModel = "model-A",
            onModelSelected = {},
            onSave = {},
            onShareLog = {},
            onClearLog = {},
            onExportLogsComplete = {}
        )
    }
}