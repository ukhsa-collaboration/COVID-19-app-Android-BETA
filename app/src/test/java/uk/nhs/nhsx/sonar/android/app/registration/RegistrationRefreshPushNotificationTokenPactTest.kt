package uk.nhs.nhsx.sonar.android.app.registration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit.PactProviderRule
import au.com.dius.pact.consumer.junit.PactVerification
import au.com.dius.pact.core.model.RequestResponsePact
import au.com.dius.pact.core.model.annotations.Pact
import au.com.dius.pact.core.model.annotations.PactFolder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.apache.http.HttpStatus.SC_BAD_REQUEST
import org.apache.http.HttpStatus.SC_NO_CONTENT
import org.apache.http.HttpStatus.SC_UNAUTHORIZED
import org.assertj.core.api.Assertions.assertThat
import org.joda.time.LocalDateTime
import org.joda.time.format.ISODateTimeFormat
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.StoppedUTCClock
import uk.nhs.nhsx.sonar.android.app.decodeBase64
import uk.nhs.nhsx.sonar.android.app.encodeBase64
import uk.nhs.nhsx.sonar.android.app.generateSignature
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.jsonObjectOf
import uk.nhs.nhsx.sonar.android.app.notifications.NotificationTokenApi
import uk.nhs.nhsx.sonar.android.app.testQueue
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@ExperimentalCoroutinesApi
@PactFolder("pacts")
class RegistrationRefreshPushNotificationTokenPactTest {

    private val sonarId: UUID = UUID.fromString("C8FBB0C6-BECF-42B2-A249-A0C2D2EA81A9")
    private val unknownSonarId: UUID = UUID.fromString("4BBEF540-4EB8-49C2-8742-AB4C9BE37FE2")

    private val secretKey: SecretKey = SecretKeySpec(
        decodeBase64(
            "dbxpjWJ+7uazW4la4o/Cu8vxFd4Yi0QNndu/gTfqx4I="
        ), "AES"
    )
    private val timestamp: String = "2020-05-20T11:27:04Z"
    private val time: LocalDateTime =
        LocalDateTime.parse(timestamp, ISODateTimeFormat.dateTimeParser())

    private val wrongTimestamp: String = "2020-05-20T11:28:04Z" // one second ahead
    private val wrongTime: LocalDateTime =
        LocalDateTime.parse(wrongTimestamp, ISODateTimeFormat.dateTimeParser())

    private val pushNotificationToken =
        "e0-3koMpK0wpuKSowc15ox:APA91bG1HCU0GTwhQ0oDsLAEF5yoCt7D8otDk9zNS2jtNCTMnGf-oIjojm-qTEZ_rAsRhANg6zmPXeUBJV03G9xS-gogySg4ieACGpoU8jxVsT4FUH6Vof8uf6i5A5_dWEo96Mz3NdXr"

    private val invalidPushNotificationToken = "invalid token" // something less than 15 chars

    private val secretKeyStorage: SecretKeyStorage = mockk<KeyStorage>(relaxed = true).apply {
        every { provideSecretKey() } returns secretKey
    }

    @get:Rule
    val provider = PactProviderRule("Registration API", this)

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @Pact(consumer = "Android App")
    fun refreshTokenSuccess(builder: PactDslWithProvider): RequestResponsePact {
        val requestBody = jsonObjectOf(
            "sonarId" to sonarId.toString(),
            "pushNotificationToken" to pushNotificationToken
        )
        return builder
            .given(
                "a confirmed registration",
                mutableMapOf<String, Any>(
                    "id" to sonarId.toString(),
                    "key" to encodeBase64(secretKey.encoded)
                )
            )
            .given("the date and time is", mutableMapOf<String, Any>("timestamp" to timestamp))
            .uponReceiving("refresh push notification token request")
            .path(
                "/api/registration/push-notification-token"
            )
            .method("PUT")
            .matchHeader(
                "Sonar-Request-Timestamp",
                "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z",
                timestamp
            )
            .headers(
                "Sonar-Message-Signature",
                generateSignature(
                    secretKey,
                    timestamp,
                    requestBody.toString().toByteArray()
                )
            )
            .body(requestBody)
            // response
            .willRespondWith()
            .status(SC_NO_CONTENT)
            .toPact()
    }

    @Test
    @PactVerification(fragment = "refreshTokenSuccess")
    fun `verifies the contract for successful push notification refresh`() {
        val api = refreshPushNotificationTokenAPI(time)

        val request = api.updateToken(
            sonarId.toString(),
            pushNotificationToken
        )

        runBlocking { request.toCoroutine() }

        assertThat(request.isSuccess).isTrue()
    }

    @Pact(consumer = "Android App")
    fun refreshTokenUnauthorized(builder: PactDslWithProvider): RequestResponsePact {
        val requestBody = jsonObjectOf(
            "sonarId" to sonarId.toString(),
            "pushNotificationToken" to pushNotificationToken
        )
        return builder
            .given(
                "a confirmed registration",
                mutableMapOf<String, Any>(
                    "id" to sonarId.toString(),
                    "key" to encodeBase64(secretKey.encoded)
                )
            )
            .given("the date and time is", mutableMapOf<String, Any>("timestamp" to timestamp))
            .uponReceiving("refresh push notification token request with bad signature")
            .path(
                "/api/registration/push-notification-token"
            )
            .method("PUT")
            .matchHeader(
                "Sonar-Request-Timestamp",
                "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z",
                timestamp
            )
            .headers(
                "Sonar-Message-Signature",
                generateSignature(secretKey, wrongTimestamp, requestBody.toString().toByteArray())
            )
            .body(requestBody)
            // response
            .willRespondWith()
            .status(SC_BAD_REQUEST)
            .toPact()
    }

    @Test
    @PactVerification(fragment = "refreshTokenUnauthorized")
    fun `verifies the contract for refresh token unauthorized`() {
        val api = refreshPushNotificationTokenAPI(wrongTime)

        val request = api.updateToken(
            sonarId.toString(),
            pushNotificationToken
        )

        runBlocking { request.toCoroutine() }

        assertThat(request.isSuccess).isFalse()
    }

    @Pact(consumer = "Android App")
    fun refreshTokenUnknownSonarId(builder: PactDslWithProvider): RequestResponsePact {
        val requestBody = jsonObjectOf(
            "sonarId" to unknownSonarId.toString(),
            "pushNotificationToken" to pushNotificationToken
        )
        return builder
            .given("the date and time is", mutableMapOf<String, Any>("timestamp" to timestamp))
            .uponReceiving("refresh push notification token request with unknown sonar id")
            .path(
                "/api/registration/push-notification-token"
            )
            .method("PUT")
            .matchHeader(
                "Sonar-Request-Timestamp",
                "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z",
                timestamp
            )
            .headers(
                "Sonar-Message-Signature",
                generateSignature(secretKey, timestamp, requestBody.toString().toByteArray())
            )
            .body(requestBody)
            // response
            .willRespondWith()
            .status(SC_BAD_REQUEST)
            .body(
                jsonObjectOf(
                    "message" to "sorry, bad request."
                )
            )
            .toPact()
    }

    @Test
    @PactVerification(fragment = "refreshTokenUnknownSonarId")
    fun `verifies the contract for refresh token unknown sonar id`() {
        val api = refreshPushNotificationTokenAPI(time)

        val request = api.updateToken(
            unknownSonarId.toString(),
            pushNotificationToken
        )

        runBlocking { request.toCoroutine() }

        assertThat(request.isSuccess).isFalse()
    }

    @Pact(consumer = "Android App")
    fun refreshTokenInvalidPushNotificationToken(builder: PactDslWithProvider): RequestResponsePact {
        val requestBody = jsonObjectOf(
            "sonarId" to sonarId.toString(),
            "pushNotificationToken" to invalidPushNotificationToken
        )
        return builder
            .given(
                "a confirmed registration",
                mutableMapOf<String, Any>(
                    "id" to sonarId.toString(),
                    "key" to encodeBase64(secretKey.encoded)
                )
            )
            .given("the date and time is", mutableMapOf<String, Any>("timestamp" to timestamp))
            .uponReceiving("refresh push notification token request with invalid push notification token")
            .path(
                "/api/registration/push-notification-token"
            )
            .method("PUT")
            .matchHeader(
                "Sonar-Request-Timestamp",
                "[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}Z",
                timestamp
            )
            .headers(
                "Sonar-Message-Signature",
                generateSignature(secretKey, timestamp, requestBody.toString().toByteArray())
            )
            .body(requestBody)
            // response
            .willRespondWith()
            .status(SC_BAD_REQUEST)
            .body(
                jsonObjectOf(
                    "message" to "sorry, bad request.",
                    "invalidFields" to arrayOf("pushNotificationToken")
                )
            )
            .toPact()
    }

    @Test
    @PactVerification(fragment = "refreshTokenInvalidPushNotificationToken")
    fun `verifies the contract for refresh token invalid push notification token`() {
        val api = refreshPushNotificationTokenAPI(time)

        val request = api.updateToken(
            sonarId.toString(),
            invalidPushNotificationToken
        )

        runBlocking { request.toCoroutine() }

        assertThat(request.isSuccess).isFalse()
    }

    private fun refreshPushNotificationTokenAPI(datetime: LocalDateTime): NotificationTokenApi {
        val httpClient =
            HttpClient(
                queue = testQueue(),
                sonarHeaderValue = "some-header",
                appVersion = "buildInfo",
                utcClock = StoppedUTCClock(datetime),
                base64enc = ::encodeBase64
            )

        val api = NotificationTokenApi(
            provider.url,
            secretKeyStorage,
            httpClient
        )
        return api
    }
}
