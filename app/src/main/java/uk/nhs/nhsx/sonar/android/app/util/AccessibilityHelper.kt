/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.content.Context
import android.content.res.Resources
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.annotation.StringRes
import timber.log.Timber

fun Context.announce(@StringRes stringRes: Int) {
    val accessibilityEvent = AccessibilityEvent.obtain().apply {
        eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
        text.add(getString(stringRes))
    }
    val accessibilityManager =
        getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager?
    if (accessibilityManager?.isEnabled == true) {
        accessibilityManager.sendAccessibilityEvent(
            accessibilityEvent
        )
    }
}

fun Context.isInversionModeEnabled(): Boolean {
    var isInversionEnabled = false
    val accessibilityEnabled = try {
        Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED
        )
    } catch (e: SettingNotFoundException) {
        Timber.d("Error finding setting ACCESSIBILITY_DISPLAY_INVERSION_ENABLED: ${e.message}")
        Timber.d("Checking negative color enabled status")
        val highContrast = "high_contrast"
        Settings.System.getInt(contentResolver, highContrast, 0)
    }
    if (accessibilityEnabled == 1) {
        Timber.d("inversion  or negative colour is enabled")
        isInversionEnabled = true
    } else {
        Timber.d("inversion  or negative colour is disabled")
    }
    return isInversionEnabled
}

val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
