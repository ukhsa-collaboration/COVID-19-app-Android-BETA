package uk.nhs.nhsx.sonar.android.app.ble

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothStatusSubscriptionHandler.CombinedStatus
import uk.nhs.nhsx.sonar.android.app.util.BluetoothNotificationHelper

class BluetoothStatusSubscriptionHandlerTest {

    private val delegate = mockk<BluetoothStatusSubscriptionHandler.Delegate>(relaxUnitFun = true)
    private val helper = mockk<BluetoothNotificationHelper>(relaxUnitFun = true)
    private val handler = BluetoothStatusSubscriptionHandler(delegate, helper)

    @Test
    fun `test handle(), with location, bluetooth and ready state`() {
        val status = CombinedStatus(
            isBleClientInReadyState = true,
            isBluetoothEnabled = true,
            isLocationEnabled = true
        )

        handler.handle(status)

        verifyAll {
            helper.hideLocationIsDisabled()
            helper.hideBluetoothIsDisabled()
            delegate.startGattAndAdvertise()
            delegate.startScan()
        }
    }

    @Test
    fun `test handle(), when location is disabled`() {
        val status = CombinedStatus(
            isBleClientInReadyState = true,
            isBluetoothEnabled = true,
            isLocationEnabled = false
        )

        handler.handle(status)

        verifyAll {
            helper.showLocationIsDisabled()
            helper.hideBluetoothIsDisabled()
            delegate.startGattAndAdvertise()
            delegate.startScan()
        }
    }

    @Test
    fun `test handle(), when bluetooth is disabled`() {
        val status = CombinedStatus(
            isBleClientInReadyState = false,
            isBluetoothEnabled = false,
            isLocationEnabled = true
        )

        handler.handle(status)

        verifyAll {
            helper.hideLocationIsDisabled()
            helper.showBluetoothIsDisabled()
            delegate.stopGattAndAdvertise()
        }
    }

    @Test
    fun `test handle(), when not in ready state`() {
        val status = CombinedStatus(
            isBleClientInReadyState = false,
            isBluetoothEnabled = true,
            isLocationEnabled = true
        )

        handler.handle(status)

        verifyAll {
            helper.hideLocationIsDisabled()
            helper.hideBluetoothIsDisabled()
        }
        verify(exactly = 0) { delegate.startGattAndAdvertise() }
        verify(exactly = 0) { delegate.startScan() }
    }
}
