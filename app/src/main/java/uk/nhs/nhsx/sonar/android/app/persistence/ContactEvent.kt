/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = ContactEvent.TABLE_NAME)
data class ContactEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteContactId: String,
    val rssi: Int,
    val timestamp: String
) {
    companion object {
        const val TABLE_NAME = "contactEvents"
    }
}
