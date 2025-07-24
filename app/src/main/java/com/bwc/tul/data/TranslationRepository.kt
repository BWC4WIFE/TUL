package com.bwc.tul.data // Corrected package

import android.content.SharedPreferences
import android.content.res.Resources
import com.bwc.tul.R

class TranslationRepository(
    private val prefs: SharedPreferences,
    private val resources: Resources
) {
    fun getSelectedModel(): String = prefs.getString("selected_model", "") ?: ""

    fun getApiVersions(): List<ApiVersion> { // ApiVersion is now resolved by package
        return resources.getStringArray(R.array.api_versions).mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) ApiVersion(parts[0].trim(), parts[1].trim())
            else null
        }
    }
    // Added a function to get ApiKeys, assuming it's needed for settings.
    fun getApiKeys(): List<ApiKeyInfo> {
        return resources.getStringArray(R.array.api_keys).mapNotNull {
            val parts = it.split(":", limit = 2)
            if (parts.size == 2) ApiKeyInfo(parts[0].trim(), parts[1].trim())
            else null
        }
    }

}