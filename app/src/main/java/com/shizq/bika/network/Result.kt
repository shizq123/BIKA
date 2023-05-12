package com.shizq.bika.network

sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val code: Int=0, val error: String = "", val message: String = "") :
        Result<Nothing>()

    object Loading : Result<Nothing>()
}