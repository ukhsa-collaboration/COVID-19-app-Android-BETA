package uk.nhs.nhsx.sonar.android.app.ble

import uk.nhs.nhsx.sonar.android.app.util.BluetoothNotificationHelper

class BluetoothStatusSubscriptionHandler(
    private val delegate: Delegate,
    private val notifications: BluetoothNotificationHelper
) {

    interface Delegate {
        fun startGattAndAdvertise()
        fun stopGattAndAdvertise()
        fun startScan()
    }

    data class CombinedStatus(
        val isBleClientInReadyState: Boolean,
        val isBluetoothEnabled: Boolean,
        val isLocationEnabled: Boolean
    )

    fun handle(status: CombinedStatus) {
        if (status.isLocationEnabled) {
            notifications.hideLocationIsDisabled()
        } else {
            notifications.showLocationIsDisabled()
        }

        if (status.isBluetoothEnabled) {
            notifications.hideBluetoothIsDisabled()
        } else {
            delegate.stopGattAndAdvertise()
            notifications.showBluetoothIsDisabled()
        }

        if (status.isBleClientInReadyState) {
            delegate.startGattAndAdvertise()
            delegate.startScan()
        }
    }
}
