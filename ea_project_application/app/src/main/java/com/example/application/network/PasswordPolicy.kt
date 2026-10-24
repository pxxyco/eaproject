package com.example.application.network

object PasswordPolicy {
    private val regex = Regex("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).{8,128}$")

    fun isValid(password: String?): Boolean = password != null && regex.matches(password)
}