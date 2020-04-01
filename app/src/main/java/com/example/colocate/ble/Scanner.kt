package com.example.colocate.ble

import kotlinx.coroutines.CoroutineScope

interface Scanner {
    fun start(coroutineScope: CoroutineScope)
    fun stop()
}
