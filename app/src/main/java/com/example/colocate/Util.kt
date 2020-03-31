/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.colocate.isolate.IsolateActivity
import com.example.colocate.status.CovidStatus

const val REQUEST_ENABLE_BT: Int = 47
const val REQUEST_LOCATION: Int = 75

fun hasLocationPermission(context: Context): Boolean {
    return checkCoarseLocationPermissions(context) == PackageManager.PERMISSION_GRANTED &&
            checkFineLocationPermissions(context) == PackageManager.PERMISSION_GRANTED
}

fun checkFineLocationPermissions(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_FINE_LOCATION
)

fun checkCoarseLocationPermissions(context: Context) = ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

fun getChannel(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        NotificationChannel(
            context.getString(R.string.default_notification_channel_id),
            context.getString(R.string.main_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).let {
            (context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(it)
        }
        NotificationCompat.Builder(
            context,
            context.getString(R.string.default_notification_channel_id)
        ).build()
    }
    return context.getString(R.string.default_notification_channel_id)
}

fun Activity.navigateTo(status: CovidStatus) {
    if (status == CovidStatus.OK && this !is OkActivity) {
        OkActivity.start(this)
        finish()
    }

    if (status == CovidStatus.POTENTIAL && this !is AtRiskActivity) {
        AtRiskActivity.start(this)
        finish()
    }

    if (status == CovidStatus.RED && this !is IsolateActivity) {
        IsolateActivity.start(this)
        finish()
    }
}
