/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.content.Context
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import com.squareup.seismic.ShakeDetector

abstract class BaseActivity : AppCompatActivity() {

    private val shakeDetector =
        ShakeDetector(ShakeDetector.Listener { TesterActivity.start(this@BaseActivity) })

    override fun onResume() {
        super.onResume()

        (getSystemService(Context.SENSOR_SERVICE) as SensorManager).also {
            shakeDetector.start(it)
        }
    }

    override fun onPause() {
        super.onPause()

        shakeDetector.stop()
    }
}
