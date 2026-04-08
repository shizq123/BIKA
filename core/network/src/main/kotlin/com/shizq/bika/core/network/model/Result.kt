package com.shizq.bika.core.network.model

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class ErrorMessage(val msg: String?) : Result<Nothing>()
}