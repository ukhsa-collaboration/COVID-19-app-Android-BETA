package com.example.colocate.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.colocate.notifications.Acknowledgement.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class Acknowledgement(@PrimaryKey val url: String) {
    companion object {
        const val TABLE_NAME = "acknowledgements"
    }
}
