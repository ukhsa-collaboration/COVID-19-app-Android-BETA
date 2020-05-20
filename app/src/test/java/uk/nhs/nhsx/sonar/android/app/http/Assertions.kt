/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.http

import com.android.volley.Request
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise
import uk.nhs.nhsx.sonar.android.app.functionaltypes.Promise.State.Explanation
import java.nio.charset.Charset

class RequestAssert(private val request: Request<*>) {
    companion object {
        fun assertThat(request: Request<*>) = RequestAssert(request)
    }

    fun bodyHasJson(vararg fields: Pair<String, Any>) =
        apply {
            val bodyContentType = request.bodyContentType
            val bodyString = request.body.toString(Charset.defaultCharset())

            assertThat(bodyContentType).`as`("check body content type").contains("application/json")
            assertThat(bodyString).`as`("check body json string").isEqualTo(jsonOf(*fields))
        }
}

class PromiseAssert<T>(val promise: Promise<T>) {

    companion object {
        fun <T> assertThat(promise: Promise<T>) = PromiseAssert(promise)
    }

    fun isInProgress(): PromiseAssert<T> =
        apply {
            assertThat(promise.isInProgress).`as`("check promise in progress").isTrue()
        }

    fun succeeded(): PromiseAssert<T> =
        apply {
            assertThat(promise.isSuccess).`as`("check promise success").isTrue()
            assertThat(promise.error).`as`("check promise has no error").isNull()
        }

    fun succeededWith(value: T): PromiseAssert<T> =
        succeeded().apply {
            assertThat(promise.value).`as`("check promise value").isEqualTo(value)
        }

    fun failed(): PromiseAssert<T> =
        apply {
            assertThat(promise.isFailed).`as`("check promise has failed").isTrue()
            assertThat(promise.value).`as`("check promise has no value").isNull()
        }

    inline fun <reified E : Exception> failedWith(message: String): PromiseAssert<T> =
        failed().apply {
            assertThat(promise.error?.exception).`as`("check promise exception type").isInstanceOf(E::class.java)
            assertThat(promise.error?.exception).`as`("check promise exception message").hasMessage(message)
        }

    inline fun <reified E : Exception> failedWith(code: Int): PromiseAssert<T> =
        failed().apply {
            assertThat(promise.error?.exception).`as`("check promise").isInstanceOf(E::class.java)
            assertThat(promise.error?.code).`as`("check promise").isEqualTo(code)
        }
}

class ExplanationAssert(val explanation: Explanation?) {

    companion object {
        fun assertThat(explanation: Explanation?) = ExplanationAssert(explanation)
    }

    fun isNull() = Assertions.assertThat(explanation).isNull()

    inline fun <reified T : Exception> hasExceptionWithMessage(message: String): ExplanationAssert =
        apply {
            assertThat(explanation?.exception).`as`("check explanation exception type").isInstanceOf(T::class.java)
            assertThat(explanation?.exception).`as`("check explanation exception message").hasMessage(message)
        }
}
