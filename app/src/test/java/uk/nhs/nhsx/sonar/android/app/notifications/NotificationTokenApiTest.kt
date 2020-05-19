package uk.nhs.nhsx.sonar.android.app.notifications

import com.android.volley.Request
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.SecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.TestQueue
import uk.nhs.nhsx.sonar.android.app.http.generateSignatureKey
import uk.nhs.nhsx.sonar.android.app.http.jsonOf
import java.nio.charset.Charset
import java.util.Base64

class NotificationTokenApiTest {

    private val requestQueue = TestQueue()
    private val baseUrl = "http://api.example.com"
    private val secretKeyStorage = mockk<SecretKeyStorage>()
    private val httpClient = HttpClient(requestQueue, "someValue") { Base64.getEncoder().encodeToString(it) }

    private val api = NotificationTokenApi(baseUrl, secretKeyStorage, httpClient)

    @Test
    fun `update token`() {
        every { secretKeyStorage.provideSecretKey() } returns generateSignatureKey()

        api.updateToken("my-sonar-id-201", "a-brand-new-token-300")

        verify { secretKeyStorage.provideSecretKey() }

        val request = requestQueue.lastRequest
        assertThat(request.url).isEqualTo("http://api.example.com/api/registration/push-notification-token")
        assertThat(request.method).isEqualTo(Request.Method.PUT)
        assertThat(request.body.toString(Charset.defaultCharset())).isEqualTo(
            jsonOf(
                "sonarId" to "my-sonar-id-201",
                "pushNotificationToken" to "a-brand-new-token-300"
            )
        )
        assertThat(request.headers).containsKey("Sonar-Request-Timestamp")
        assertThat(request.headers).containsKey("Sonar-Message-Signature")
    }
}
