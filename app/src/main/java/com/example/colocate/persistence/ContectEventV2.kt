package com.example.colocate.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = ContactEventV2.TABLE_NAME)
data class ContactEventV2(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sonarId: String,
    val rssiValues: List<Int>,
    val timestamp: String,
    val duration: Long
) {
    companion object {
        const val TABLE_NAME = "contactEventsV2"
    }
}
