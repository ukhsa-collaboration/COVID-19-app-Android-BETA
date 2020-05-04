/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package testsupport

import android.content.Intent

class TestIntent(private val actionValue: String?) : Intent() {
    override fun getAction(): String? = actionValue
}
