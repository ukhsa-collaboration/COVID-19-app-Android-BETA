/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

interface ResidentIdProvider {
    fun getResidentId(): String
    fun setResidentId(residentId: String)
}
