package com.dentalflow.myapplication.data.local

import android.content.Context

class SessionManager(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveDisplayName(name: String?) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name ?: "").apply()
    }

    fun getDisplayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)

    fun saveRole(role: String?) {
        prefs.edit().putString(KEY_ROLE, role ?: "").apply()
    }

    fun getRole(): String? = prefs.getString(KEY_ROLE, null)

    companion object {
        private const val PREFS = "dentalflow_session"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROLE = "role"

        fun clear(context: Context) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().clear().apply()
        }
    }
}
