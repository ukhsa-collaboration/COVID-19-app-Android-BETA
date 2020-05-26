/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.inbox

import android.content.Context
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceStringProvider
import javax.inject.Inject

class UserInbox @Inject constructor(
    private val testInfoProvider: TestInfoProvider,
    private val recoveryProvider: RecoveryProvider
) {

    fun addTestInfo(testInfo: TestInfo): Unit = testInfoProvider.set(testInfo)

    fun hasTestInfo(): Boolean = testInfoProvider.has()

    fun getTestInfo(): TestInfo = testInfoProvider.get()

    fun dismissTestInfo(): Unit = testInfoProvider.clear()

    fun addRecovery(): Unit = recoveryProvider.set("Recovery")

    fun hasRecovery(): Boolean = recoveryProvider.has()

    fun dismissRecovery(): Unit = recoveryProvider.clear()
}

class TestInfoProvider @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<TestInfo>(
        context,
        preferenceName = "user_test_info_storage",
        preferenceKey = "user_test_info",
        serialize = TestInfoSerialization::serialize,
        deserialize = TestInfoSerialization::deserialize
    )

class RecoveryProvider @Inject constructor(context: Context) :
    SharedPreferenceStringProvider(
        context,
        preferenceName = "user_recovery_storage",
        preferenceKey = "user_recovery"
    )
