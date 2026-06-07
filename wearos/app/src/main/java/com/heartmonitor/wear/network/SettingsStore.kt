package com.heartmonitor.wear.network

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("heart_monitor", Context.MODE_PRIVATE)

    fun backendUrl(): String {
        return prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
    }

    fun userId(): String {
        return prefs.getString(KEY_USER_ID, "demo-user") ?: "demo-user"
    }

    fun region(): String {
        return prefs.getString(KEY_REGION, "global") ?: "global"
    }

    companion object {
        private const val KEY_BACKEND_URL = "backend_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_REGION = "region"
        private const val DEFAULT_BACKEND_URL = "https://example.com"
    }
}
