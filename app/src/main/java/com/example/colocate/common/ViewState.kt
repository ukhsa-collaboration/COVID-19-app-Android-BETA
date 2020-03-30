package com.example.colocate.common

sealed class ViewState {
    object Progress : ViewState()
    object Success : ViewState()
    data class Error(val e: Exception) : ViewState()
}
