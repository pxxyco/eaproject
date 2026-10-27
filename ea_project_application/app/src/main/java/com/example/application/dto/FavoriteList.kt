package com.example.application.dto

data class FavoriteList(
    val id: Long,
    val name: String,
    val visibility: ListVisibility = ListVisibility.PRIVATE,
    val itemCount: Int = 0
)

