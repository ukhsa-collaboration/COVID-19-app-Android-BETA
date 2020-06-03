/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.content.Context
import android.content.res.Resources
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ScrollView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.R

fun Context.announce(@StringRes textToAnnounceRes: Int) {
    val text = getString(textToAnnounceRes)
    announce(text)
}

fun Context.announce(textToAnnounce: String) {
    val accessibilityEvent = AccessibilityEvent.obtain().apply {
        eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
        text.add(textToAnnounce)
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

fun Context.smallestScreenWidth(): Int = resources.configuration.smallestScreenWidthDp

fun MaterialCardView.cardColourInversion(isOn: Boolean) {
    when (isOn) {
        true -> {
            strokeWidth = 3.dpToPx
            strokeColor = context.getColor(R.color.black)
        }
        false -> strokeWidth = 0
    }
}

fun AppCompatActivity.setNavigateUpToolbar(
    toolbar: MaterialToolbar,
    @StringRes title: Int,
    @DrawableRes homeIndicator: Int = R.drawable.ic_arrow_back_blue
) {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.setHomeAsUpIndicator(homeIndicator)
    supportActionBar?.setHomeActionContentDescription(R.string.go_back)
    supportActionBar?.title = getString(title)
    toolbar.setNavigationOnClickListener { onBackPressed() }
}

/**
 * Reads headings for devices with api 19+.
 * This is handling accessibility headings in a better way
 * than just using the xml attribute => (android:accessibilityHeading="true")
 * because this attribute only works for api 28+.
 */
fun View.readOutAccessibilityHeading() {
    ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
        override fun onInitializeAccessibilityNodeInfo(
            host: View?,
            info: AccessibilityNodeInfoCompat
        ) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            info.isHeading = true
        }
    })
}

fun ScrollView.scrollToView(view: View) {
    post { smoothScrollTo(0, view.top) }
}
