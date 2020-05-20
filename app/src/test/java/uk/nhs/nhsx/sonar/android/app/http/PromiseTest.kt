/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.Deferred
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.State.Explanation
import uk.nhs.nhsx.sonar.android.app.http.ExplanationAssert.Companion.assertThat
import uk.nhs.nhsx.sonar.android.app.http.PromiseAssert.Companion.assertThat
import java.io.IOException

class PromiseTest {

    @Test
    fun `test success handling, when subscribing before resolution`() {
        var successValue: String? = null
        var otherSuccessValue: String? = null
        var errorValue: Explanation? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise).isInProgress()

        promise
            .onSuccess { successValue = it }
            .onSuccess { otherSuccessValue = it }
            .onError { errorValue = it }
        deferred.resolve("success!!")

        assertThat(promise).succeededWith("success!!")
        assertThat(otherSuccessValue).isEqualTo("success!!")
        assertThat(successValue).isEqualTo("success!!")
        assertThat(errorValue).isNull()
    }

    @Test
    fun `test success handling, when subscribing after resolution`() {
        var successValue: String? = null
        var errorValue: Explanation? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise).isInProgress()

        deferred.resolve("success!!")
        assertThat(promise).succeededWith("success!!")

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
        assertThat(successValue).isEqualTo("success!!")
        assertThat(errorValue).isNull()
    }

    @Test
    fun `test error handling, when subscribing before resolution`() {
        var successValue: String? = null
        var errorValue: Explanation? = null
        var otherErrorValue: Explanation? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise).isInProgress()

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
            .onError { otherErrorValue = it }
        deferred.fail(IOException("Oops"))

        assertThat(promise).failedWith<IOException>("Oops")
        assertThat(successValue).isNull()
        assertThat(errorValue).hasExceptionWithMessage<IOException>("Oops")
        assertThat(otherErrorValue).hasExceptionWithMessage<IOException>("Oops")
    }

    @Test
    fun `test error handling, when subscribing after resolution`() {
        var successValue: String? = null
        var errorValue: Explanation? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise).isInProgress()

        deferred.fail(IOException("Oops"))
        assertThat(promise).failedWith<IOException>("Oops")

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
        assertThat(successValue).isNull()
        assertThat(errorValue).hasExceptionWithMessage<IOException>("Oops")
    }

    @Test
    fun `test promise triggers each success callback only once`() {
        var count = 0
        var otherCount = 0

        val deferred = Deferred<Int>()
        val promise = deferred.promise

        promise.onSuccess { count += 1 }
        assertThat(count).isEqualTo(0)

        deferred.resolve(1)
        assertThat(count).isEqualTo(1)

        promise.onSuccess { otherCount += 1 }
        assertThat(otherCount).isEqualTo(1)
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun `test promise triggers each error callback only once`() {
        var count = 0
        var otherCount = 0

        val deferred = Deferred<Int>()
        val promise = deferred.promise

        promise.onError { count += 1 }
        assertThat(count).isEqualTo(0)

        deferred.fail(Exception())
        assertThat(count).isEqualTo(1)

        promise.onError { otherCount += 1 }
        assertThat(otherCount).isEqualTo(1)
        assertThat(count).isEqualTo(1)
    }
}
