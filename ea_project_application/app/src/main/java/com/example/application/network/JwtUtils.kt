package com.example.application.network

import android.util.Base64
import com.google.gson.JsonParser

object JwtUtils {
    private fun payload(token: String) = try {
        val part = token.split(".").getOrNull(1) ?: return null
        val json = String(Base64.decode(part, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING))
        JsonParser.parseString(json).asJsonObject
    } catch (e: Exception) { null }

    fun extractEmail(token: String): String? = payload(token)?.get("sub")?.asString
    fun extractRole(token: String): String? = payload(token)?.get("role")?.asString

    fun isExpired(token: String): Boolean {
        val exp = payload(token)?.get("exp")?.asLong ?: return true
        return System.currentTimeMillis() >= exp * 1000
    }
}