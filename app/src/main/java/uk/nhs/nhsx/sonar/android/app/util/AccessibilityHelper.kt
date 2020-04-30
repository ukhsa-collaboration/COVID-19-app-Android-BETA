package uk.nhs.nhsx.sonar.android.app.util

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import androidx.annotation.StringRes

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
