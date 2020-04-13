/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.sonar.android.app.debug.TesterActivity
import uk.nhs.nhsx.sonar.android.app.util.ShakeListener

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var shakeListener: ShakeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shakeListener = ShakeListener(this) {
            TesterActivity.start(this@BaseActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        shakeListener.start()
    }

    override fun onPause() {
        super.onPause()
        shakeListener.stop()
    }
}
