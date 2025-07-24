package com.bwc.tul.ui.dialog

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.bwc.tul.R
import com.bwc.tul.data.TranslationRepository
import com.bwc.tul.ui.settings.DevSettingsContent
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme
import java.io.File

@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    listener: DevSettingsListener,
    prefs: SharedPreferences,
    models: List<String>
) {
    val context = LocalContext.current
    val repository = TranslationRepository(prefs, context.resources)
    val apiVersions = repository.getApiVersions()

    // State variables
    var apiHost by rememberSaveable {
        mutableStateOf(prefs.getString("api_host", "generativelanguage.googleapis.com") ?: "")
    }
    var selectedApiVersion by rememberSaveable {
        mutableStateOf(prefs.getString("api_version", apiVersions.firstOrNull()?.value ?: "v1alpha") ?: "v1alpha")
    }
    var vadSensitivity by rememberSaveable {
        mutableStateOf(prefs.getInt("vad_sensitivity_ms", 800).toString())
    }
    var selectedModel by rememberSaveable {
        mutableStateOf(prefs.getString("selected_model", models.firstOrNull() ?: "").orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(text = "Developer Settings", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            ThaiUncensoredLanguageTheme {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    DevSettingsContent(
                        apiHost = apiHost,
                        onApiHostChange = { apiHost = it },
                        availableApiVersions = apiVersions,
                        selectedApiVersion = selectedApiVersion,
                        onApiVersionChange = { selectedApiVersion = it },
                        vadSensitivity = vadSensitivity,
                        onVadSensitivityChange = { vadSensitivity = it },
                        availableModels = models,
                        selectedModel = selectedModel,
                        onModelSelected = { selectedModel = it },
                        onShareLog = { listener.onShareLog() },
                        onClearLog = { listener.onClearLog() },
                        onExportLogsComplete = { file -> listener.onExportLogsComplete(file) }
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        with(prefs.edit()) {
                            putString("api_host", apiHost)
                            putString("api_version", selectedApiVersion)
                            putString("selected_model", selectedModel)
                            putInt("vad_sensitivity_ms", vadSensitivity.toIntOrNull() ?: 800)
                            apply()
                        }
                        listener.onSettingsSaved()
                        onDismissRequest()
                    }
                ) {
                    Text("Save")
                }
            }
        }
    )
}

interface DevSettingsListener {
    fun onForceConnect()
    fun onShareLog()
    fun onClearLog()
    fun onSettingsSaved()
    fun onExportLogsComplete(file: File)
}