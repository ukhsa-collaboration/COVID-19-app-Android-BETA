/*
 * Copyright © 2020 NHSX. All rights reserved.
 *
 */

package uk.nhs.nhsx.sonar.android.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import org.awaitility.kotlin.await
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

// IMPORTANT
// =========
// See the README.md to configure how to setup a mani in the middle and to make these tests pass
@RunWith(AndroidJUnit4::class)
class CertificatePinningIT {
    @Test
    @Ignore
    fun shouldReturnPinningErrorWhenCallingNHSEndpointWithAManInTheMiddle() {
        var expectedError: Exception? = null
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        ResidentApi(
            VolleyHttpClient(
                "https://sonar-colocate-services.apps.cp.data.england.nhs.uk",
                appContext
            )
            )
            .register { error ->
                expectedError = error
            }

        await.until { expectedError !== null }
        assertTrue(expectedError?.message?.contains("Pin verification failed")!!)
    }
}