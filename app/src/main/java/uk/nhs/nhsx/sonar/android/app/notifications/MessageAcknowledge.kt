/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.notifications

import javax.inject.Inject

class MessageAcknowledge @Inject constructor(
    private val acknowledgmentsDao: AcknowledgmentsDao,
    private val acknowledgmentsApi: AcknowledgmentsApi
) {
    fun hasBeenAcknowledged(message: NotificationMessage) =
        message.acknowledgmentUrl?.let {
            acknowledgmentsDao.tryFind(it) != null
        } ?: false

    fun acknowledgeIfNecessary(message: NotificationMessage) =
        message.acknowledgmentUrl?.let {
            val acknowledgment = Acknowledgment(it)
            acknowledgmentsApi.send(acknowledgment.url)
            acknowledgmentsDao.insert(acknowledgment)
        }
}
