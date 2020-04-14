/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.contactevents

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactEventV2Dao {
    @Insert
    fun insert(contactEvent: ContactEventV2)

    @Query("SELECT * FROM ${ContactEventV2.TABLE_NAME}")
    suspend fun getAll(): List<ContactEventV2>

    @Query("DELETE FROM ${ContactEventV2.TABLE_NAME}")
    suspend fun clearEvents()

    @Query("DELETE FROM ${ContactEventV2.TABLE_NAME} WHERE timestamp < :timestamp")
    suspend fun clearOldEvents(timestamp: String)
}
