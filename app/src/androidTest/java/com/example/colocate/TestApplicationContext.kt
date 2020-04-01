package com.example.colocate

import androidx.core.os.bundleOf
import androidx.test.rule.ActivityTestRule
import com.example.colocate.di.module.AppModule
import com.example.colocate.di.module.BluetoothModule
import com.example.colocate.di.module.NetworkModule
import com.example.colocate.di.module.PersistenceModule
import com.example.colocate.di.module.StatusModule
import com.example.colocate.persistence.ID_NOT_REGISTERED
import com.example.colocate.registration.TokenRetriever
import com.google.firebase.messaging.RemoteMessage
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import uk.nhs.nhsx.sonar.android.client.di.EncryptionKeyStorageModule
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class TestApplicationContext(rule: ActivityTestRule<FlowTestStartActivity>) {

    private val notificationService = NotificationService()
    private val testModule = TestModule()
    private val testDispatcher = TestCoLocateServiceDispatcher()
    private val testActivity = rule.activity
    private val app = rule.activity.application as ColocateApplication
    private val mockServer = MockWebServer()

    init {
        mockServer.apply {
            dispatcher = testDispatcher
            start()
        }

        val mockServerUrl = mockServer.url("").toString().removeSuffix("/")

        app.appComponent =
            DaggerTestAppComponent.builder()
                .persistenceModule(PersistenceModule(app))
                .bluetoothModule(BluetoothModule(app))
                .appModule(AppModule(app))
                .encryptionKeyStorageModule(EncryptionKeyStorageModule(app))
                .statusModule(StatusModule(app))

                .networkModule(NetworkModule(mockServerUrl))
                .testModule(testModule)
                .build()

        app.appComponent.inject(notificationService)
    }

    fun shutdownMockServer() {
        mockServer.shutdown()
    }

    fun simulateActivationCodeReceived() {
        val msg = RemoteMessage(bundleOf("activationCode" to "test activation code #001"))
        notificationService.onMessageReceived(msg)
    }

    fun verifyReceivedRegistrationRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices/registrations")
        assertThat(lastRequest?.body?.readUtf8()).isEqualTo("""{"pushToken":"test firebase token #010"}""")
    }

    fun verifyReceivedActivationRequest() {
        val lastRequest = mockServer.takeRequest(500, TimeUnit.MILLISECONDS)

        assertThat(lastRequest).isNotNull()
        assertThat(lastRequest?.method).isEqualTo("POST")
        assertThat(lastRequest?.path).isEqualTo("/api/devices")
        assertThat(lastRequest?.body?.readUtf8()).isEqualTo("""{"activationCode":"test activation code #001","pushToken":"test firebase token #010"}""")
    }

    fun verifyResidentIdAndSecretKey() {
        val idProvider = testActivity.residentIdProvider
        val keyStorage = testActivity.encryptionKeyStorage

        waitUntil {
            idProvider.getResidentId() != ID_NOT_REGISTERED
        }
        assertThat(idProvider.getResidentId()).isEqualTo(TestCoLocateServiceDispatcher.RESIDENT_ID)

        waitUntil {
            keyStorage.provideKey() != null
        }
        val decodedKey = keyStorage.provideKey()?.toString(Charset.defaultCharset())
        assertThat(decodedKey).isEqualTo(TestCoLocateServiceDispatcher.SECRET_KEY)
    }
}

class TestTokenRetriever : TokenRetriever {
    override suspend fun retrieveToken() =
        TokenRetriever.Result.Success("test firebase token #010")
}

private fun waitUntil(predicate: () -> Boolean) {
    val maxAttempts = 20
    var attempts = 1

    while (!predicate() && attempts <= maxAttempts) {
        Thread.sleep(100)
        attempts++
    }

    if (!predicate()) {
        fail<String>("Failed waiting for predicate")
    }
}
