package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet.bottomDialogFirstCta
import kotlinx.android.synthetic.main.bottom_sheet.bottomDialogSecondCta
import kotlinx.android.synthetic.main.bottom_sheet.bottomDialogText
import kotlinx.android.synthetic.main.bottom_sheet.bottomDialogTitle
import uk.nhs.nhsx.sonar.android.app.R

class BottomDialog(
    context: Context,
    configuration: BottomDialogConfiguration,
    onCancel: () -> Unit = {},
    onFirstCtaClick: () -> Unit = {},
    onSecondCtaClick: () -> Unit = {}
) : BottomSheetDialog(context, R.style.PersistentBottomSheet) {
    init {
        setContentView(layoutInflater.inflate(R.layout.bottom_sheet, null))
        behavior.isHideable = configuration.isHideable

        bottomDialogTitle.setText(configuration.titleResId)
        bottomDialogText.setText(configuration.textResId)
        bottomDialogSecondCta.setText(configuration.secondCtaResId)
        if (configuration.firstCtaResId != null) {
            bottomDialogFirstCta.setText(configuration.firstCtaResId)
            bottomDialogFirstCta.isVisible = true
        }

        bottomDialogFirstCta.setOnClickListener {
            onFirstCtaClick()
            dismiss()
        }

        bottomDialogSecondCta.setOnClickListener {
            onSecondCtaClick()
            dismiss()
        }

        setOnCancelListener {
            onCancel()
            dismiss()
        }
    }

    fun setTitleResId(@StringRes titleResId: Int) {
        bottomDialogTitle.setText(titleResId)
    }

    fun setTextResId(@StringRes textResId: Int) {
        bottomDialogText.setText(textResId)
    }

    fun setFirstCtaVisibility(visible: Boolean) {
        bottomDialogFirstCta.isVisible = visible
    }
}

data class BottomDialogConfiguration(
    val isHideable: Boolean,
    @StringRes val titleResId: Int,
    @StringRes val textResId: Int,
    @StringRes val firstCtaResId: Int? = null,
    @StringRes val secondCtaResId: Int
)
