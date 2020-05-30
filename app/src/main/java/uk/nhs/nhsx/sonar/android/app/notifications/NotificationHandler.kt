/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.inbox.TestResult
import javax.inject.Inject

class NotificationHandler @Inject constructor(
    private val exposureMessageHandler: ExposureMessageHandler,
    private val activationCodeMessageHandler: ActivationCodeMessageHandler,
    private val testResultMessageHandler: TestResultMessageHandler,
    private val messageAcknowledge: MessageAcknowledge
) {

    fun handleNewMessage(messageData: Map<String, String>) {

        val message = buildMessageFrom(messageData)

        if (!messageAcknowledge.hasBeenAcknowledged(message)) {
            message.handle()
        }

        messageAcknowledge.acknowledgeIfNecessary(message)
    }

    private fun buildMessageFrom(data: Map<String, String>): NotificationMessage {
        val acknowledgmentUrl = data[ACKNOWLEDGMENT_URL]
        return when {
            isContactAlert(data) ->
                ExposureMessage(
                    handler = this.exposureMessageHandler,
                    acknowledgmentUrl = acknowledgmentUrl,
                    date = DateTime(data[EXPOSURE_DATE_KEY])

                )
            isActivation(data) ->
                ActivationCodeMessage(
                    handler = this.activationCodeMessageHandler,
                    acknowledgmentUrl = acknowledgmentUrl,
                    code = data.getValue(ACTIVATION_CODE_KEY)
                )
            isTestResult(data) ->
                TestResultMessage(
                    handler = this.testResultMessageHandler,
                    acknowledgmentUrl = data[ACKNOWLEDGMENT_URL],
                    result = TestResult.valueOf(data.getValue(TEST_RESULT_KEY)),
                    date = DateTime(data.getValue(TEST_RESULT_DATE_KEY))
                )
            else -> UnknownMessage(acknowledgmentUrl = acknowledgmentUrl)
        }
    }

    companion object {

        private fun isContactAlert(data: Map<String, String>) =
            data[TYPE_KEY] == TYPE_STATUS_UPDATE && data[EXPOSURE_KEY] == EXPOSURE_VALUE

        private fun isActivation(data: Map<String, String>) =
            data.containsKey(ACTIVATION_CODE_KEY)

        private fun isTestResult(data: Map<String, String>) =
            data[TYPE_KEY] == TYPE_TEST_RESULT &&
                data.containsKey(TEST_RESULT_KEY) &&
                data.containsKey(TEST_RESULT_DATE_KEY)

        private const val TYPE_KEY = "type"
        private const val TYPE_STATUS_UPDATE = "Status Update"
        private const val TYPE_TEST_RESULT = "Test Result"

        private const val EXPOSURE_KEY = "status"
        private const val EXPOSURE_VALUE = "Potential"
        private const val EXPOSURE_DATE_KEY = "mostRecentProximityEventDate"

        private const val TEST_RESULT_KEY = "result"
        private const val TEST_RESULT_DATE_KEY = "testTimestamp"
        private const val ACTIVATION_CODE_KEY = "activationCode"
        private const val ACKNOWLEDGMENT_URL = "acknowledgmentUrl"
    }
}

abstract class NotificationMessage {
    abstract val acknowledgmentUrl: String?

    abstract fun handle()
}

data class ExposureMessage(
    val handler: ExposureMessageHandler,
    override val acknowledgmentUrl: String?,
    val date: DateTime
) : NotificationMessage() {
    override fun handle() = handler.handle(this)
}

data class ActivationCodeMessage(
    val handler: ActivationCodeMessageHandler,
    override val acknowledgmentUrl: String?,
    val code: String
) : NotificationMessage() {
    override fun handle() = handler.handle(this)
}

data class TestResultMessage(
    val handler: TestResultMessageHandler,
    override val acknowledgmentUrl: String?,
    val result: TestResult,
    val date: DateTime
) : NotificationMessage() {
    override fun handle() = handler.handle(this)
}

data class UnknownMessage(
    override val acknowledgmentUrl: String?
) : NotificationMessage() {

    override fun handle() = Unit
}
