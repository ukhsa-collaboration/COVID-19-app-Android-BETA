package uk.nhs.nhsx.sonar.android.app.testhelpers

import android.content.ContextWrapper
import androidx.core.os.bundleOf
import com.google.firebase.messaging.RemoteMessage
import uk.nhs.nhsx.sonar.android.app.SonarApplication
import uk.nhs.nhsx.sonar.android.app.inbox.TestInfo
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationService
import uk.nhs.nhsx.sonar.android.app.util.toUtcIsoFormat

class TestNotificationEmulator(app: SonarApplication) {

    val notificationService = NotificationService()

    init {
        replaceAppNotificationService(app)
    }

    private fun replaceAppNotificationService(app: SonarApplication) {
        val contextField = ContextWrapper::class.java.getDeclaredField("mBase")
        contextField.isAccessible = true
        contextField.set(notificationService, app)
    }

    fun simulateExposureNotificationReceived() {
        val msg = RemoteMessage(
            bundleOf(
                "type" to "Status Update",
                "status" to "Potential"
            )
        )
        notificationService.onMessageReceived(msg)
    }

    fun simulateTestResultNotificationReceived(testInfo: TestInfo) {
        val msg = RemoteMessage(
            bundleOf(
                "type" to "Test Result",
                "result" to "${testInfo.result}",
                "testTimestamp" to testInfo.date.toUtcIsoFormat()
            )
        )
        notificationService.onMessageReceived(msg)
    }

    fun simulateActivationCodeReceived() {
        val msg = RemoteMessage(
            bundleOf("activationCode" to "test activation code #001")
        )
        notificationService.onMessageReceived(msg)
    }
}
