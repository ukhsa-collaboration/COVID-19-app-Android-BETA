package uk.nhs.nhsx.sonar.android.app

import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import uk.nhs.nhsx.sonar.android.app.util.isInversionModeEnabled

open class ColorInversionAwareActivity : AppCompatActivity {

    constructor() : super()

    constructor(@LayoutRes layoutId: Int) : super(layoutId)

    override fun onResume() {
        super.onResume()
        handleInversion(isInversionModeEnabled())
    }

    open fun handleInversion(inversionModeEnabled: Boolean) {
    }
}
