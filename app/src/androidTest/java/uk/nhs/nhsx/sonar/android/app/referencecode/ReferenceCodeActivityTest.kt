/*
 * Copyright © 2020 NHSX. All rights reserved.
 */
package uk.nhs.nhsx.sonar.android.app.referencecode

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.sonar.android.app.EspressoJunit5Test
import uk.nhs.nhsx.sonar.android.app.startTestActivity

class ReferenceCodeActivityTest : EspressoJunit5Test() {

    private val referenceCodeRobot = ReferenceCodeRobot()

    @Test
    fun testShowsReferenceCode() {
        testAppContext.setFullValidUser()
        testAppContext.app.startTestActivity<ReferenceCodeActivity>()
        referenceCodeRobot.checkReferenceCodeIs("REF CODE #202")
    }
}
