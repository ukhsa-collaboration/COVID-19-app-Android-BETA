/*
 * Copyright © 2020 NHSX. All rights reserved.
 */
package uk.nhs.nhsx.sonar.android.app.referencecode

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.startTestActivity

@RunWith(AndroidJUnit4::class)
class ReferenceCodeActivityTest : EspressoTest() {

    private val referenceCodeRobot = ReferenceCodeRobot()

    @Test
    fun testShowsReferenceCode() {
        testAppContext.setFullValidUser()
        testAppContext.app.startTestActivity<ReferenceCodeActivity>()
        referenceCodeRobot.checkReferenceCodeIs("REF CODE #202")
    }
}
