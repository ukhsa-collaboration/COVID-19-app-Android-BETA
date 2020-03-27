/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

const val ID_NOT_REGISTERED = "00000000-0000-0000-0000-000000000000"

interface ResidentIdProvider {
    fun getResidentId(): String
    fun hasProperResidentId(): Boolean
    fun setResidentId(residentId: String)
}
