package testsupport

import android.content.Intent

class TestIntent(private val actionValue: String?) : Intent() {
    override fun getAction(): String? = actionValue
}
