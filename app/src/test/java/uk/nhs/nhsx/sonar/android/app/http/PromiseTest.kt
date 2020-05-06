/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.Promise.Deferred
import java.io.IOException

class PromiseTest {

    @Test
    fun `test success handling, when subscribing before resolution`() {
        var successValue: String? = null
        var otherSuccessValue: String? = null
        var errorValue: Exception? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise.isInProgress).isTrue()

        promise
            .onSuccess { successValue = it }
            .onSuccess { otherSuccessValue = it }
            .onError { errorValue = it }
        deferred.resolve("success!!")

        assertThat(promise.isSuccess).isTrue()
        assertThat(promise.value).isEqualTo("success!!")
        assertThat(promise.error).isNull()
        assertThat(otherSuccessValue).isEqualTo("success!!")
        assertThat(successValue).isEqualTo("success!!")
        assertThat(errorValue).isNull()
    }

    @Test
    fun `test success handling, when subscribing after resolution`() {
        var successValue: String? = null
        var errorValue: Exception? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise.isInProgress).isTrue()

        deferred.resolve("success!!")
        assertThat(promise.isSuccess).isTrue()
        assertThat(promise.value).isEqualTo("success!!")
        assertThat(promise.error).isNull()

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
        assertThat(successValue).isEqualTo("success!!")
        assertThat(errorValue).isNull()
    }

    @Test
    fun `test error handling, when subscribing before resolution`() {
        var successValue: String? = null
        var errorValue: Exception? = null
        var otherErrorValue: Exception? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise.isInProgress).isTrue()

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
            .onError { otherErrorValue = it }
        deferred.fail(IOException("Oops"))

        assertThat(promise.isFailed).isTrue()
        assertThat(promise.value).isNull()
        assertThat(promise.error).isInstanceOf(IOException::class.java)
        assertThat(promise.error).hasMessage("Oops")
        assertThat(successValue).isNull()
        assertThat(errorValue).isInstanceOf(IOException::class.java)
        assertThat(errorValue).hasMessage("Oops")
        assertThat(otherErrorValue).isInstanceOf(IOException::class.java)
        assertThat(otherErrorValue).hasMessage("Oops")
    }

    @Test
    fun `test error handling, when subscribing after resolution`() {
        var successValue: String? = null
        var errorValue: Exception? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise.isInProgress).isTrue()

        deferred.fail(IOException("Oops"))
        assertThat(promise.isFailed).isTrue()
        assertThat(promise.value).isNull()
        assertThat(promise.error).isInstanceOf(IOException::class.java)
        assertThat(promise.error).hasMessage("Oops")

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
        assertThat(successValue).isNull()
        assertThat(errorValue).isInstanceOf(IOException::class.java)
        assertThat(errorValue).hasMessage("Oops")
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
