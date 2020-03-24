/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

const val TABLE_NAME = "contactEvents"

@JsonClass(generateAdapter = true)
@Entity(tableName = TABLE_NAME)
data class ContactEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteContactId: String,
    val rssi: Int,
    val timestamp: String
)
