/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.ble

import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
data class ContactEvent(
    val uuid: String
)