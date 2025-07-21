package com.bwc.translator2.ui.dialog

import android.view.LayoutInflater
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import com.bwc.translator2.ui.settings.DevSettingsContent
import com.bwc.translator2.ui.theme.ThaiUncensoredLanguageTheme

// MODIFICATION: This class now uses Compose for its content.
class SettingsDialog(
    context: Context,
    private val listener: DevSettingsListener,
    private val prefs: SharedPreferences,
    private val models: List<String>
) : Dialog(context) {

    interface DevSettingsListener {
        fun onForceConnect()
        fun onShareLog()
        fun onClearLog()
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)

        // MODIFICATION: Using ComposeView instead of XML
        val composeView = ComposeView(context)
        setContentView(composeView)

        composeView.setContent {
            // State is hoisted here in the Dialog class
            var apiHost by remember {
                mutableStateOf(prefs.getString("api_host",
                    "generativelanguage.googleapis.com") ?: "")
            }
            var apiVersion by remember {
                mutableStateOf(prefs.getString("api_version", "v1alpha") ?: "")
            }
            var vadSensitivity by remember {
                mutableStateOf(prefs.getInt("vad_sensitivity_ms", 800)
                    .toString())
            }
            var selectedModel by remember {
                mutableStateOf(prefs.getString("selected_model", models.first())
                    ?: models.first())
            }

            ThaiUncensoredLanguageTheme {
                DevSettingsContent(
                    apiHost = apiHost,
                    onApiHostChange = { apiHost = it },
                    apiVersion = apiVersion,
                    onApiVersionChange = { apiVersion = it },
                    vadSensitivity = vadSensitivity,
                    onVadSensitivityChange = { vadSensitivity = it },
                    availableModels = models,
                    selectedModel = selectedModel,
                    onModelSelected = { selectedModel = it },
                    onSave = {
                        with(prefs.edit()) {
                            putString("api_host", apiHost)
                            putString("api_version", apiVersion)
                            putString("selected_model", selectedModel)
                            putInt("vad_sensitivity_ms",
                                vadSensitivity.toIntOrNull() ?: 800)
                            apply()
                        }
                        dismiss()
                    },
                    onShareLog = { listener.onShareLog() },
                    onClearLog = { listener.onClearLog() }
                )
            }
        }
    }
}