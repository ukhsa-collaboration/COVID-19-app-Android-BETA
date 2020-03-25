package com.example.colocate.persistence

interface KeyProvider {

    fun getKey(): ByteArray
}