package uk.nhs.nhsx.sonar.android.app.referencecode

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.showAndExpand

class ReferenceCodeDialog(
    activity: ComponentActivity,
    viewModel: ReferenceCodeViewModel,
    openButton: View
) : BottomSheetDialog(activity, R.style.PersistentBottomSheet) {

    init {
        setContentView(layoutInflater.inflate(R.layout.bottom_sheet_reference_code, null))

        val referenceCodeView = findViewById<TextView>(R.id.reference_code)!!
        val closeButton = findViewById<Button>(R.id.close)!!

        closeButton.setOnClickListener { dismiss() }
        setOnCancelListener { dismiss() }
        openButton.setOnClickListener { showAndExpand() }

        viewModel.state().observe({ activity.lifecycle }) { state ->
            referenceCodeView.text =
                when (state) {
                    ReferenceCodeViewModel.State.Loading -> activity.getString(R.string.loading)
                    is ReferenceCodeViewModel.State.Loaded -> state.code.value
                    ReferenceCodeViewModel.State.Error -> activity.getString(R.string.error)
                }
        }
    }
}
