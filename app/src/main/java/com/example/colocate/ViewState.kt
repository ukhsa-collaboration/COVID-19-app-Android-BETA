package com.example.colocate

sealed class ViewState {
    object Progress : ViewState()
    object Success : ViewState()
    data class Error(val e: Exception) : ViewState()
}
