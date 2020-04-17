/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AcknowledgementsDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(acknowledgement: Acknowledgement)

    @Query("SELECT * FROM ${Acknowledgement.TABLE_NAME} WHERE url = :url")
    fun tryFind(url: String): Acknowledgement?
}
