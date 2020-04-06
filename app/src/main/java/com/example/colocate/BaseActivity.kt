/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.colocate.debug.TesterActivity
import com.example.colocate.util.ShakeListener

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
