/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactEventV2Dao {
    @Insert
    fun insert(contactEvent: ContactEventV2)

    @Query("SELECT * FROM ${ContactEventV2.TABLE_NAME}")
    suspend fun getAll(): List<ContactEventV2>
}
