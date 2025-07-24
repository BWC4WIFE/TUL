package com.bwc.tul.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.DialogFragment
import com.bwc.tul.ui.settings.UserSettingsContent
import com.bwc.tul.ui.theme.ThaiUncensoredLanguageTheme

class UserSettingsDialogFragment : DialogFragment() {

    interface UserSettingsListener {
        fun onSettingsSaved()
        fun onRequestPermission()
    }

    private var listener: UserSettingsListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? UserSettingsListener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // MODIFICATION: Replaced XML inflation with ComposeView
        return ComposeView(requireContext()).apply {
            setContent {
                // State is managed and "hoisted" here in the DialogFragment
                val prefs = requireActivity().getSharedPreferences(
                    "BwctransPrefs", Context.MODE_PRIVATE)

                var apiKey by remember {
                    mutableStateOf(prefs.getString("api_key", "") ?: "")
                }
                var sourceLang by remember {
                    mutableStateOf(prefs.getString("source_lang", "en-US") ?: "en-US")
                }
                var targetLang by remember {
                    mutableStateOf(prefs.getString("target_lang", "es-ES") ?: "es-ES")
                }

                ThaiUncensoredLanguageTheme {
                    UserSettingsContent(
                        apiKey = apiKey,
                        onApiKeyChange = { apiKey = it },
                        sourceLang = sourceLang,
                        onSourceLangChange = { sourceLang = it },
                        targetLang = targetLang,
                        onTargetLangChange = { targetLang = it },
                        onSave = {
                            with(prefs.edit()) {
                                putString("api_key", apiKey)
                                putString("source_lang", sourceLang)
                                putString("target_lang", targetLang)
                                apply()
                            }
                            listener?.onSettingsSaved()
                            dismiss()
                        },
                        onDismiss = {
                            dismiss()
                        }
                    )
                }
            }
        }
    }
}