package uk.nhs.nhsx.sonar.android.app.ble

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat

class BleEventTracker : BleEvents {

    private val eventsList = mutableListOf<ConnectedDevice>()

    private val connectionEvents = MutableLiveData<List<ConnectedDevice>>()

    override fun observeConnectionEvents(): LiveData<List<ConnectedDevice>> =
        connectionEvents

    private val lock = Object()

    override fun connectedDeviceEvent(id: String, rssiValues: List<Int>) {
        safelyUpdateEventList {
            eventsList.removeIf { it.id == id }
            eventsList.add(
                ConnectedDevice(
                    id = id,
                    timestamp = getCurrentTimeStamp(),
                    rssiValues = rssiValues
                )
            )
        }
    }

    override fun disconnectDeviceEvent(id: String?) {
        safelyUpdateEventList {
            eventsList.removeIf { it.id == id }
            eventsList.add(ConnectedDevice(id = id, isConnectionError = true))
        }
    }

    override fun scanFailureEvent() {
        safelyUpdateEventList {
            eventsList.add(ConnectedDevice(isReadFailure = true))
        }
    }

    override fun clear() {
        safelyUpdateEventList {
            eventsList.clear()
        }
    }

    private fun safelyUpdateEventList(codeBlock: () -> Unit) {
        val eventListCopy = synchronized(lock) {
            codeBlock()
            mutableListOf<ConnectedDevice>().apply { addAll(eventsList) }
        }
        connectionEvents.postValue(eventListCopy)
    }
}

interface BleEvents {

    fun observeConnectionEvents(): LiveData<List<ConnectedDevice>>

    fun connectedDeviceEvent(id: String, rssiValues: List<Int>)

    fun disconnectDeviceEvent(id: String? = null)

    fun scanFailureEvent()

    fun clear()
}

fun getCurrentTimeStamp() = DateTime.now(DateTimeZone.UTC).toUtcIsoFormat()

data class ConnectedDevice(
    val id: String? = null,
    val timestamp: String = "",
    val rssiValues: List<Int> = emptyList(),
    val isConnectionError: Boolean = false,
    val isReadFailure: Boolean = false
)
