package com.bwc.tul.ui.dialog

import android.content.SharedPreferences
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import com.bwc.tul.ui.settings.UserSettingsContent
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme

@Composable
fun UserSettingsDialog(
    prefs: SharedPreferences,
    onDismiss: () -> Unit,
    onSave: (apiKey: String, sourceLang: String, targetLang: String) -> Unit
) {
    var apiKey by remember {
        mutableStateOf(prefs.getString("api_key", "") ?: "")
    }
    var sourceLang by remember {
        mutableStateOf(prefs.getString("source_lang", "en-US") ?: "en-US")
    }
    var targetLang by remember {
        mutableStateOf(prefs.getString("target_lang", "th-TH") ?: "th-TH")
    }

    Dialog(onDismissRequest = onDismiss) {
        ThaiUncensoredLanguageTheme {
            UserSettingsContent(
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it },
                sourceLang = sourceLang,
                onSourceLangChange = { sourceLang = it },
                targetLang = targetLang,
                onTargetLangChange = { targetLang = it },
                onSave = {
                    onSave(apiKey, sourceLang, targetLang)
                },
                onDismiss = onDismiss
            )
        }
    }
}