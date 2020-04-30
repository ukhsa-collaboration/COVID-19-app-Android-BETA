package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.security.keystore.KeyInfo
import android.util.Base64
import androidx.test.rule.ActivityTestRule
import junit.framework.TestCase.fail
import org.awaitility.kotlin.await
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import uk.nhs.nhsx.sonar.android.app.http.AndroidSecretKeyStorage
import uk.nhs.nhsx.sonar.android.app.http.PREF_SECRET_KEY
import uk.nhs.nhsx.sonar.android.app.http.PUBLIC_KEY_FILENAME
import uk.nhs.nhsx.sonar.android.app.http.SECRET_KEY_PREFERENCE_FILENAME
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.Security
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory

private const val b64EncodedSecretKey = "5e359QJt4+iunhd7Op5/AQ=="
private const val someOtherB64SecretKey = "8O4yb62a/zMXvkUnxkgCtQ=="
private val message = "somthing to sign".toByteArray()
private val expectedMessageSignature =
    Base64.decode("lALk5pvISLja72Od1kmRHMd9GR7Z47PJgrN+QSW61H8=", Base64.DEFAULT)

const val exampleServerPubPEM = """-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEu1f68MqDXbKeTqZMTHsOGToO4rKn
PClXe/kE+oWqlaWZQv4J1E98cUNdpzF9JIFRPMCNdGOvTr4UB+BhQv9GWg==
-----END PUBLIC KEY-----"""

class AndroidSecretKeyStorageTest {

    @get:Rule
    val activityRule = ActivityTestRule(FlowTestStartActivity::class.java)

    @get:Rule
    val exceptionRule: ExpectedException = ExpectedException.none()

    private val context by lazy { activityRule.activity.applicationContext }
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Test
    fun testAll() {
        val tests = listOf(
            ::providesNoSecretKeyWhenNoneHasBeenStored,
            ::storedKeysAreInsideSecureHardware,
            ::storedSecretKeyCanBeUsedForSigningWithHMACSHA256,
            ::storedKeysAreOverriddenWhenStoredAgain,
            ::storedSecretKeyCannotBeUsedForSigningWithOtherAlgorithms,
            ::migratesExistingKeysInSharedPrefs,
            ::willNotMigrateTheKeyWhenOneAlreadyExistsInTheKeyStore
        )

        tests.forEach {
            setUp()
            it()
        }
    }

    fun setUp() {
        keyStore.aliases().asSequence().forEach { keyStore.deleteEntry(it) }
        listOf(PUBLIC_KEY_FILENAME, SECRET_KEY_PREFERENCE_FILENAME).forEach {
            val clear = context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear()
            if (!clear.commit()) fail("Unable to clear shared preference: $it")
        }
    }

    private fun createStorage() = AndroidSecretKeyStorage(keyStore, context)

    fun providesNoSecretKeyWhenNoneHasBeenStored() {
        val keyStorage = createStorage()

        val storedKey = keyStorage.provideSecretKey()
        assertThat(storedKey, nullValue())
    }

    fun storedKeysAreInsideSecureHardware() {
        val storedKey = createStorage().apply {
            storeSecretKey(b64EncodedSecretKey)
        }.provideSecretKey()!!

        val keyFactory = SecretKeyFactory.getInstance(storedKey.getAlgorithm(), "AndroidKeyStore")
        val keyInfo = keyFactory.getKeySpec(storedKey, KeyInfo::class.java) as KeyInfo
        assertThat(keyInfo.isInsideSecureHardware, equalTo(true))
    }

    fun storedSecretKeyCanBeUsedForSigningWithHMACSHA256() {
        val storedKey = createStorage().apply {
            storeSecretKey(b64EncodedSecretKey)
        }.provideSecretKey()!!

        val signature = Mac.getInstance("HMACSHA256").apply {
            init(storedKey)
        }.doFinal(message)

        assertThat(signature, equalTo(expectedMessageSignature))
    }

    fun storedKeysAreOverriddenWhenStoredAgain() {
        val storedKey = createStorage().apply {
            storeSecretKey(b64EncodedSecretKey)
            storeSecretKey(someOtherB64SecretKey)
        }.provideSecretKey()!!

        val signature = Mac.getInstance("HMACSHA256").apply {
            init(storedKey)
        }.doFinal(message)

        assertThat(signature, not(equalTo(expectedMessageSignature)))
    }

    fun storedSecretKeyCannotBeUsedForSigningWithOtherAlgorithms() {
        val storedKey = createStorage().apply {
            storeSecretKey(b64EncodedSecretKey)
        }.provideSecretKey()!!

        exceptionRule.expect(InvalidKeyException::class.java)
        Mac.getInstance(
            "HMACSHA512",
            Security.getProvider("AndroidKeyStoreBCWorkaround") // Force the use of AndroidKeyStore over BC
        ).apply {
            init(storedKey)
        }
    }

    fun migratesExistingKeysInSharedPrefs() {
        context.getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_SECRET_KEY, b64EncodedSecretKey)
            .apply()

        val storedKey = createStorage().provideSecretKey()
        val signature = Mac.getInstance("HMACSHA256").apply {
            init(storedKey)
        }.doFinal(message)

        assertThat(signature, equalTo(expectedMessageSignature))
        val prefs = context
            .getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .all

        await.atMost(200, MILLISECONDS).until { prefs.isEmpty() }
    }

    fun willNotMigrateTheKeyWhenOneAlreadyExistsInTheKeyStore() {
        context.getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_SECRET_KEY, someOtherB64SecretKey)
            .apply()

        val storedKey = createStorage().apply {
            storeSecretKey(b64EncodedSecretKey)
        }.provideSecretKey()

        val signature = Mac.getInstance("HMACSHA256").apply {
            init(storedKey)
        }.doFinal(message)

        assertThat(signature, equalTo(expectedMessageSignature))
        val prefs = context
            .getSharedPreferences(SECRET_KEY_PREFERENCE_FILENAME, Context.MODE_PRIVATE)
            .all

        await.atMost(200, MILLISECONDS).until { prefs.isEmpty() }
    }
}
