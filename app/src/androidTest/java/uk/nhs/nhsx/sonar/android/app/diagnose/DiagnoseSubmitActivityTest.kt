package uk.nhs.nhsx.sonar.android.app.diagnose

import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE
import uk.nhs.nhsx.sonar.android.app.testhelpers.base.EspressoTest
import uk.nhs.nhsx.sonar.android.app.testhelpers.robots.DiagnoseSubmitRobot

class DiagnoseSubmitActivityTest : EspressoTest() {

    private val diagnoseSubmitRobot = DiagnoseSubmitRobot()

    @Test
    fun confirmationIsRequired() {
        startTestActivity<DiagnoseSubmitActivity> {
            putSymptoms(setOf(COUGH, TEMPERATURE))
            putExtra("SYMPTOMS_DATE", DateTime.now().millis)
        }

        diagnoseSubmitRobot.submit()
        diagnoseSubmitRobot.checkConfirmationIsNeeded()
    }
}
