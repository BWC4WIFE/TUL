package com.bwc.tul.ui.dialog

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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

    Dialog(onDismissRequest = onDismissRequest) {
        ThaiUncensoredLanguageTheme {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Developer Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))

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
                        onSave = {
                            with(prefs.edit()) {
                                putString("api_host", apiHost)
                                putString("api_version", selectedApiVersion)
                                putString("selected_model", selectedModel)
                                putInt("vad_sensitivity_ms", vadSensitivity.toIntOrNull() ?: 800)
                                apply()
                            }
                            listener.onSettingsSaved()
                            onDismissRequest()
                        },
                        onShareLog = { listener.onShareLog() },
                        onClearLog = { listener.onClearLog() },
                        onExportLogsComplete = { file -> listener.onExportLogsComplete(file) }
                    )
                }
            }
        }
    }
}

interface DevSettingsListener {
    fun onSettingsSaved()
    fun onShareLog()
    fun onClearLog()
    fun onExportLogsComplete(file: File)
}