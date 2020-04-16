package uk.nhs.nhsx.sonar.android.client.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.client.http.Promise.Deferred
import java.io.IOException

class PromiseTest {

    @Test
    fun testSuccessHandling_WhenSubscribingBeforeResolution() {
        var successValue: String? = null
        var errorValue: Exception? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise.isInProgress).isTrue()

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
        deferred.resolve("success!!")

        assertThat(promise.isSuccess).isTrue()
        assertThat(promise.value).isEqualTo("success!!")
        assertThat(promise.error).isNull()
        assertThat(successValue).isEqualTo("success!!")
        assertThat(errorValue).isNull()
    }

    @Test
    fun testSuccessHandling_WhenSubscribingAfterResolution() {
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
    fun testErrorHandling_WhenSubscribingBeforeResolution() {
        var successValue: String? = null
        var errorValue: Exception? = null

        val deferred = Deferred<String>()
        val promise = deferred.promise

        assertThat(promise.isInProgress).isTrue()

        promise
            .onSuccess { successValue = it }
            .onError { errorValue = it }
        deferred.fail(IOException("Oops"))

        assertThat(promise.isFailed).isTrue()
        assertThat(promise.value).isNull()
        assertThat(promise.error).isInstanceOf(IOException::class.java)
        assertThat(promise.error).hasMessage("Oops")
        assertThat(successValue).isNull()
        assertThat(errorValue).isInstanceOf(IOException::class.java)
        assertThat(errorValue).hasMessage("Oops")
    }

    @Test
    fun testErrorHandling_WhenSubscribingAfterResolution() {
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
    fun testPromiseTriggersOnlyOnce() {
        var count = 0

        val deferred = Deferred<Int>()
        val promise = deferred.promise

        deferred.resolve(1)
        promise.onSuccess { count += 1 }.onError { }

        assertThat(count).isEqualTo(1)
    }
}
