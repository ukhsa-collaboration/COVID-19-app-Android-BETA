package uk.nhs.nhsx.sonar.android.app

import org.junit.After
import org.junit.Before
import uk.nhs.nhsx.sonar.android.app.testhelpers.TestApplicationContext

abstract class EspressoTest {

    protected lateinit var testAppContext: TestApplicationContext

    @Before
    fun setup() {
        testAppContext = TestApplicationContext()
        testAppContext.reset()
    }

    @After
    fun teardown() {
        testAppContext.shutdownMockServer()
    }
}
