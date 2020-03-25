/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

import java.util.UUID

interface ResidentIdProvider {
    fun getResidentId(): String
}
