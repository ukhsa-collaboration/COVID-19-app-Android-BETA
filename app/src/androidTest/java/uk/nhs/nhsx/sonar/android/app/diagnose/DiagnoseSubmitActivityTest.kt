package uk.nhs.nhsx.sonar.android.app.diagnose

import org.joda.time.DateTime
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.EspressoTest
import uk.nhs.nhsx.sonar.android.app.status.Symptom.COUGH
import uk.nhs.nhsx.sonar.android.app.status.Symptom.TEMPERATURE

class DiagnoseSubmitActivityTest : EspressoTest() {

    private val diagnoseSubmitRobot = DiagnoseSubmitRobot()

    private fun startActivity() {
        testAppContext.app.startTestActivity<DiagnoseSubmitActivity> {
            putSymptoms(setOf(COUGH, TEMPERATURE))
            putExtra("SYMPTOMS_DATE", DateTime.now().millis)
        }
    }

    @Test
    fun confirmationIsRequired() {
        startActivity()

        diagnoseSubmitRobot.submit()
        diagnoseSubmitRobot.checkConfirmationIsNeeded()
    }
}
