package com.example.application.network

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object TokenStorage {
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
    }

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun saveRefreshToken(token: String) = prefs.edit { putString("refresh_token", token) }

    fun clear() = prefs.edit { clear() }
}