/*
 * Copyright © 2020 NHSX. All rights reserved.
 */
package uk.nhs.nhsx.sonar.android.app.referencecode

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest

class ReferenceCodeActivityTest : EspressoTest() {

    private val referenceCodeRobot = ReferenceCodeRobot()

    @Test
    fun showsReferenceCode() {
        testAppContext.setFullValidUser()
        testAppContext.app.startTestActivity<ReferenceCodeActivity>()
        referenceCodeRobot.checkReferenceCodeIs("REF CODE #202")
    }
}
