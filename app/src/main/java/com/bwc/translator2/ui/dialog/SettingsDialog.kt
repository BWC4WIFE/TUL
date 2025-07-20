package com.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.Toast
import com.bwc.translator2.R
import com.bwc.translator2.data.ApiKeyInfo
import com.bwc.translator2.data.ApiVersion
import com.bwc.translator2.databinding.DialogSettingsBinding

class SettingsDialog(
    context: Context,
    private val listener: DevSettingsListener,
    private val prefs: SharedPreferences,
    private val models: List<String>
) : Dialog(context) {

    interface DevSettingsListener {
        fun onForceConnect()
        fun onSettingsSaved()
        fun onShareLog()
        fun onClearLog()
    }

    companion object {
        private const val TAG = "SettingsDialog"
        private const val DEFAULT_API_HOST = "generativelanguage.googleapis.com"
    }

    private lateinit var binding: DialogSettingsBinding
    private var apiVersionsList: List<ApiVersion> = emptyList()
    private var apiKeysList: List<ApiKeyInfo> = emptyList()
    private var selectedApiVersion: ApiVersion? = null
    private var selectedApiKeyInfo: ApiKeyInfo? = null
    private var selectedModel: String = models.firstOrNull() ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCancelable(true)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        loadAndSetCurrentValues()
        setupViews()
    }

    private fun loadAndSetCurrentValues() {
        // Load API Versions with improved error handling
        apiVersionsList = try {
            context.resources.getStringArray(R.array.api_versions).mapNotNull {
                val parts = it.split("|", limit = 2)
                when {
                    parts.size == 2 -> ApiVersion(parts[0].trim(), parts[1].trim())
                    it.isNotBlank() -> ApiVersion(it.trim(), it.trim()).also {
                        Log.w(TAG, "API version item missing '|' delimiter: '$it'")
                    }
                    else -> null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading API versions", e)
            emptyList()
        }

        // Load API Keys with improved error handling
        apiKeysList = try {
            context.resources.getStringArray(R.array.api_keys).mapNotNull {
                val parts = it.split(":", limit = 2)
                if (parts.size == 2) {
                    ApiKeyInfo(parts[0].trim(), parts[1].trim())
                } else {
                    Log.e(TAG, "Malformed API key item: '$it'. Expected 'DisplayName:Value' format.")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading API keys", e)
            emptyList()
        }

        selectedApiVersion = apiVersionsList.firstOrNull { it.value == prefs.getString("api_version", null) }
        selectedApiKeyInfo = apiKeysList.firstOrNull { it.value == prefs.getString("api_key", null) }
        selectedModel = prefs.getString("selected_model", models.firstOrNull()) ?: models.firstOrNull() ?: ""
    }

    private fun setupViews() {
        // Connection Settings
        binding.hostManualEditText.setText(prefs.getString("api_host", DEFAULT_API_HOST) ?: DEFAULT_API_HOST)

        // API Version Selection
        apiVersionsList.forEach { apiVersion ->
            binding.apiVersionRadioGroup.addView(RadioButton(context).apply {
                text = apiVersion.displayName
                tag = apiVersion
                isChecked = apiVersion == selectedApiVersion
            })
        }
        binding.apiVersionRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            selectedApiVersion = group.findViewById<RadioButton>(checkedId).tag as? ApiVersion
        }

        // API Key Selection
        binding.apiKeyManualEditText.setText(prefs.getString("api_key", "") ?: "")
        apiKeysList.forEach { apiKey ->
            binding.apiKeyRadioGroup.addView(RadioButton(context).apply {
                text = apiKey.displayName
                tag = apiKey
                isChecked = apiKey.value == prefs.getString("api_key", null)
            })
        }
        binding.apiKeyRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            (group.findViewById<RadioButton>(checkedId).tag as? ApiKeyInfo)?.let {
                binding.apiKeyManualEditText.setText(it.value)
            }
        }

        // Model Selection
        binding.modelManualEditText.setText(selectedModel)
        models.forEach { model ->
            binding.modelRadioGroup.addView(RadioButton(context).apply {
                text = model
                tag = model
                isChecked = model == selectedModel
            })
        }
        binding.modelRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            (group.findViewById<RadioButton>(checkedId).tag as? String)?.let {
                binding.modelManualEditText.setText(it)
            }
        }

        // VAD Sensitivity
        val currentVad = prefs.getInt("vad_sensitivity_ms", 800)
        binding.vadSensitivity.progress = currentVad
        binding.vadValue.text = "$currentVad ms"
        binding.vadSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.vadValue.text = "$progress ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Debug Options
        binding.debugOverlaySwitch.isChecked = prefs.getBoolean("show_debug_overlay", false)

        // Log Actions
        binding.shareLogBtn.setOnClickListener {
            listener.onShareLog()
        }
        binding.clearLogBtn.setOnClickListener {
            listener.onClearLog()
            Toast.makeText(context, "On-screen log cleared", Toast.LENGTH_SHORT).show()
        }

        // Save Actions
        binding.saveSettingsBtn.setOnClickListener {
            saveSettings()
            dismiss()
        }
        binding.forceConnectBtn.setOnClickListener {
            saveSettings()
            listener.onForceConnect()
            dismiss()
        }
    }

    private fun saveSettings() {
        prefs.edit().apply {
            putString("api_host", binding.hostManualEditText.text.toString().trim())
            putString("api_key", binding.apiKeyManualEditText.text.toString().trim())
            putString("selected_model", binding.modelManualEditText.text.toString().trim())
            selectedApiVersion?.let { putString("api_version", it.value) }
            putInt("vad_sensitivity_ms", binding.vadSensitivity.progress)
            putBoolean("show_debug_overlay", binding.debugOverlaySwitch.isChecked)
            apply()
        }
        Log.d(TAG, "Settings saved")
        listener.onSettingsSaved()
    }
}
