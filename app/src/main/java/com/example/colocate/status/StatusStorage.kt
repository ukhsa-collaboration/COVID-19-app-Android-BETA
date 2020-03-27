/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.status

interface StatusStorage {
    fun update(status: CovidStatus)
    fun get(): CovidStatus
}

enum class CovidStatus {
    OK,
    POTENTIAL,
    RED
}
