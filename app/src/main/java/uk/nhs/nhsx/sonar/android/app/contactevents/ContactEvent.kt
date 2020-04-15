package uk.nhs.nhsx.sonar.android.app.contactevents

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import uk.nhs.nhsx.sonar.android.app.ble.Identifier

@JsonClass(generateAdapter = true)
@Entity(tableName = ContactEvent.TABLE_NAME)
data class ContactEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val sonarId: ByteArray,
    val rssiValues: List<Int>,
    val timestamp: Long,
    val duration: Int
) {
    companion object {
        const val TABLE_NAME = "contactEventsV2"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContactEvent

        if (id != other.id) return false
        if (!sonarId.contentEquals(other.sonarId)) return false
        if (rssiValues != other.rssiValues) return false
        if (timestamp != other.timestamp) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sonarId.contentHashCode()
        result = 31 * result + rssiValues.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + duration.hashCode()
        return result
    }

    override fun toString(): String {
        return "ContactEvent(sonarid=${idAsString()}, rssiValues=${rssiValues.joinToString(",","[","]")}, timestamp=$timestamp, duration=$duration"
    }

    fun idAsString(): String = Identifier.fromBytes(sonarId).asString
}
