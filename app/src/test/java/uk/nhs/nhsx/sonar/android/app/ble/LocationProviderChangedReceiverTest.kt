package uk.nhs.nhsx.sonar.android.app.ble

import android.app.Activity
import android.content.Intent
import android.location.LocationManager.GPS_PROVIDER
import android.location.LocationManager.NETWORK_PROVIDER
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper

class LocationProviderChangedReceiverTest {

    private val location = mockk<LocationHelper>()
    private val receiver = LocationProviderChangedReceiver(location)
    private val context = Activity()

    private var latestStatus: Boolean? = null

    @Before
    fun setUp() {
        receiver.getLocationStatus().subscribe { latestStatus = it }
    }

    @Test
    fun `test onReceive() with providers changed action, gps enabled and network disabled`() {
        val intent = TestIntent("android.location.PROVIDERS_CHANGED")

        every { location.isProviderEnabled(GPS_PROVIDER) } returns true
        every { location.isProviderEnabled(NETWORK_PROVIDER) } returns false

        receiver.onReceive(context, intent)

        verifyAll {
            location.isProviderEnabled(GPS_PROVIDER)
            location.isProviderEnabled(NETWORK_PROVIDER)
        }
        assertThat(latestStatus).isTrue()
    }

    @Test
    fun `test onReceive() with providers changed action, gps disabled and network enabled`() {
        val intent = TestIntent("android.location.PROVIDERS_CHANGED")

        every { location.isProviderEnabled(GPS_PROVIDER) } returns false
        every { location.isProviderEnabled(NETWORK_PROVIDER) } returns true

        receiver.onReceive(context, intent)

        assertThat(latestStatus).isTrue()
    }

    @Test
    fun `test onReceive() with providers changed action, gps disabled and network disabled`() {
        val intent = TestIntent("android.location.PROVIDERS_CHANGED")

        every { location.isProviderEnabled(GPS_PROVIDER) } returns false
        every { location.isProviderEnabled(NETWORK_PROVIDER) } returns false

        receiver.onReceive(context, intent)

        assertThat(latestStatus).isFalse()
    }

    @Test
    fun `test onReceive() with unknown action`() {
        val intent = TestIntent("foo bar")

        receiver.onReceive(context, intent)

        assertThat(latestStatus).isNull()
    }

    @Test
    fun `test onReceive() with null action`() {
        val intent = TestIntent(null)

        receiver.onReceive(context, intent)

        assertThat(latestStatus).isNull()
    }
}

class TestIntent(private val actionValue: String?) : Intent() {
    override fun getAction(): String? = actionValue
}
