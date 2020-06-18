/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.interstitials

import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.CurrentAdviceRobot

class CurrentAdviceActivityTest : EspressoTest() {

    private val currentAdviceRobot = CurrentAdviceRobot()

    @Test
    fun showsAdviceSpecificForStateWithoutUntilDate() {
        startActivityWithState<CurrentAdviceActivity>()

        currentAdviceRobot.checkIconIsShowing()
        currentAdviceRobot.checkActivityIsDisplayed()
        currentAdviceRobot.checkCorrectStateIsDisplay(testData.defaultState)
        currentAdviceRobot.checkAdviceUrlIsDisplayed()
    }

    @Test
    fun showsAdviceSpecificForStateWithUntilDate() {
        startActivityWithState<CurrentAdviceActivity>(testData.symptomaticState)

        currentAdviceRobot.checkIconIsShowing()
        currentAdviceRobot.checkActivityIsDisplayed()
        currentAdviceRobot.checkCorrectStateIsDisplay(testData.symptomaticState)
        currentAdviceRobot.checkAdviceUrlIsDisplayed()
    }
}
