package com.example.colocate.persistence

import java.util.*

interface ResidentIdProvider {
    fun getResidentId(): UUID
}