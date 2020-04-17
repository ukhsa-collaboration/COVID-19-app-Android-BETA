/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import androidx.room.Entity
import androidx.room.PrimaryKey
import uk.nhs.nhsx.sonar.android.app.notifications.Acknowledgement.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class Acknowledgement(@PrimaryKey val url: String) {
    companion object {
        const val TABLE_NAME = "acknowledgements"
    }
}
