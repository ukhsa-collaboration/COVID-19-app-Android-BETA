package uk.nhs.nhsx.sonar.android.app

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import uk.nhs.nhsx.sonar.android.app.util.readOutAccessibilityHeading

class AccessibilityTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        readOutAccessibilityHeading()
    }
}
