package com.tapconnect.data.local

import android.content.Context
import com.tapconnect.TapConnectApplication

object TokenManager {
    private const val PREFS_NAME = "tap_connect_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"

    private val prefs by lazy {
        TapConnectApplication.getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(): String? {
        return prefs.getString(KEY_USER_ID, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_ID).apply()
    }
}
