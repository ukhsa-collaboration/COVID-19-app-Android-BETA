/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.receivers

import android.app.Activity
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import testsupport.TestIntent
import uk.nhs.nhsx.sonar.android.app.util.LocationHelper

class LocationProviderChangedReceiverTest {

    private val location = mockk<LocationHelper>()
    private val receiver = LocationProviderChangedReceiver(location)
    private val context = Activity()

    private var latestStatus: Boolean? = null

    @Before
    fun setUp() {
        receiver.getLocationStatus().subscribe { latestStatus = it }
        every { location.providerChangedIntentAction } returns "android.location.PROVIDERS_CHANGED"
    }

    @Test
    fun `test onReceive() with providers changed action, location access enabled`() {
        val intent = TestIntent("android.location.PROVIDERS_CHANGED")

        every { location.isLocationEnabled() } returns true

        receiver.onReceive(context, intent)

        assertThat(latestStatus).isTrue()
    }

    @Test
    fun `test onReceive() with providers changed action, location access disabled`() {
        val intent = TestIntent("android.location.PROVIDERS_CHANGED")

        every { location.isLocationEnabled() } returns false

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
