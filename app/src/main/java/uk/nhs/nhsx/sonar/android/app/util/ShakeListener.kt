/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.debug.TesterActivity
import kotlin.math.abs

class ShakeListener(context: Context, val onShake: () -> Unit) :
    SensorEventListener, LifecycleObserver {

    private val sensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastUpdate = System.currentTimeMillis()
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val curTime = System.currentTimeMillis()
        // only allow one update every 100ms.
        if (curTime - lastUpdate > 100) {
            val diffTime: Long = curTime - lastUpdate
            lastUpdate = curTime
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val speed: Float =
                abs(x + y + z - lastX - lastY - lastZ) / diffTime * 2000
            if (speed > SHAKE_THRESHOLD) {
                Timber.d("shake detected w/ speed: $speed")
                onShake()
            }
            lastX = x
            lastY = y
            lastZ = z
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    companion object {
        private const val SHAKE_THRESHOLD = 800
    }
}

fun Application.registerShakeDetector() {
    this.registerActivityLifecycleCallbacks(
        object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStarted(activity: Activity) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is BaseActivity) {
                    val shakeListener = ShakeListener(
                        applicationContext
                    ) {
                        TesterActivity.start(activity)
                    }
                    activity.lifecycle.addObserver(shakeListener)
                }
            }

            override fun onActivityResumed(activity: Activity) {
            }
        }
    )
}
