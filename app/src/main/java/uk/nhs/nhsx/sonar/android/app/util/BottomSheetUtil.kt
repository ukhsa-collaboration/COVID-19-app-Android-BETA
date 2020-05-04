/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

fun BottomSheetDialog.showExpanded() {
    show()
    behavior.state = BottomSheetBehavior.STATE_EXPANDED
}
