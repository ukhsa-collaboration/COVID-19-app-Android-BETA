/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContactEventDao {
    @Insert
    fun insert(contactEvent: ContactEvent)

    @Query("SELECT * FROM ${ContactEvent.TABLE_NAME}")
    suspend fun getAll(): List<ContactEvent>

    @Query("DELETE FROM ${ContactEvent.TABLE_NAME}")
    suspend fun clearEvents()
}
