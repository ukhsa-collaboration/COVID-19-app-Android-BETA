package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet.bottomDialogCtaButton
import kotlinx.android.synthetic.main.bottom_sheet.bottomDialogText
import kotlinx.android.synthetic.main.bottom_sheet.bottomDialogTitle
import uk.nhs.nhsx.sonar.android.app.R

class BottomDialog(
    context: Context,
    configuration: BottomDialogConfiguration,
    onCancel: () -> Unit = {},
    onButtonClick: () -> Unit
) : BottomSheetDialog(context, R.style.PersistentBottomSheet) {
    init {
        setContentView(layoutInflater.inflate(R.layout.bottom_sheet, null))
        behavior.isHideable = configuration.isHideable

        bottomDialogTitle.setText(configuration.titleResId)
        bottomDialogText.setText(configuration.textResId)
        bottomDialogCtaButton.setText(configuration.ctaButtonTextResId)

        bottomDialogCtaButton.setOnClickListener {
            onButtonClick()
            dismiss()
        }
        setOnCancelListener {
            onCancel()
            dismiss()
        }
    }


}

data class BottomDialogConfiguration(
    @StringRes val titleResId: Int,
    @StringRes val textResId: Int,
    @StringRes val ctaButtonTextResId: Int,
    val isHideable: Boolean
)
