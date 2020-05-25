/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.inbox

import android.content.Context
import uk.nhs.nhsx.sonar.android.app.util.SharedPreferenceSerializingProvider
import javax.inject.Inject

class UserInbox @Inject constructor(private val testInfoProvider: TestInfoProvider) {

    fun addTestResult(testInfo: TestInfo): Unit = testInfoProvider.set(testInfo)

    fun hasTestResult(): Boolean = testInfoProvider.has()

    fun dismissTestResult(): Unit = testInfoProvider.clear()
}

class TestInfoProvider @Inject constructor(context: Context) :
    SharedPreferenceSerializingProvider<TestInfo>(
        context,
        preferenceName = "user_test_info_storage",
        preferenceKey = "user_test_info",
        serialize = TestInfoSerialization::serialize,
        deserialize = TestInfoSerialization::deserialize
    )
