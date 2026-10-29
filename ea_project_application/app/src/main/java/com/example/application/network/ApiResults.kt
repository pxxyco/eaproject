package com.example.application.network

internal fun <T> ApiResult<T>.requireData(): T = when (this) {
    is ApiResult.Success -> data
    else -> throw IllegalStateException(accountError())
}
