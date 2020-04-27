package uk.nhs.nhsx.sonar.android.app.medicalworkers

import androidx.activity.ComponentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.bottom_sheet_medical_workers_instructions.closeButton
import uk.nhs.nhsx.sonar.android.app.R

class MedicalWorkersInstructionsDialog(
    activity: ComponentActivity
) : BottomSheetDialog(activity, R.style.PersistentBottomSheet) {

    init {
        setContentView(
            layoutInflater.inflate(
                R.layout.bottom_sheet_medical_workers_instructions,
                null
            )
        )

        closeButton.setOnClickListener { dismiss() }
        setOnCancelListener { dismiss() }
    }
}
