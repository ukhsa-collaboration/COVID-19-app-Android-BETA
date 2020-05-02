/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_isolate.book_test_card
import kotlinx.android.synthetic.main.activity_isolate.follow_until
import kotlinx.android.synthetic.main.activity_isolate.latest_advice_red
import kotlinx.android.synthetic.main.activity_isolate.nhs_service
import kotlinx.android.synthetic.main.activity_isolate.symptomsTextView
import kotlinx.android.synthetic.main.banner.toolbar_info
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeDialog
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeViewModel
import uk.nhs.nhsx.sonar.android.app.util.CheckInReminderNotification
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_LATEST_ADVICE_RED
import uk.nhs.nhsx.sonar.android.app.util.URL_SUPPORT_RED
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import uk.nhs.nhsx.sonar.android.app.util.showExpanded
import uk.nhs.nhsx.sonar.android.app.util.toUiFormat
import javax.inject.Inject

class IsolateActivity : BaseActivity() {

    @Inject
    protected lateinit var userStateStorage: UserStateStorage

    @Inject
    protected lateinit var checkInReminderNotification: CheckInReminderNotification

    @Inject
    protected lateinit var referenceCodeViewModelFactory: ViewModelFactory<ReferenceCodeViewModel>

    private lateinit var updateSymptomsDialog: BottomSheetDialog

    private val referenceCodeViewModel: ReferenceCodeViewModel by viewModels { referenceCodeViewModelFactory }
    private var referenceCodeDialog: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_isolate)
        BluetoothService.start(this)

        val state = userStateStorage.get()
        val symptoms = state.symptoms().sortedDescending()

        symptomsTextView.text = symptoms.joinToString("\n") {
            when (it) {
                Symptom.TEMPERATURE -> getString(R.string.high_temperature)
                Symptom.COUGH -> getString(R.string.continuous_cough)
            }
        }

        follow_until.text = getString(R.string.follow_until, state.until.toUiFormat())

        latest_advice_red.setOnClickListener {
            openUrl(URL_LATEST_ADVICE_RED)
        }

        nhs_service.setOnClickListener {
            openUrl(URL_SUPPORT_RED)
        }

        book_test_card.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            val bookTestPhoneNumber = getString(R.string.test_booking_number)
            intent.data = Uri.parse("tel:$bookTestPhoneNumber")
            startActivity(intent)
        }

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        setUpdateSymptomsDialog()
        referenceCodeDialog = ReferenceCodeDialog(
            this,
            referenceCodeViewModel,
            findViewById(R.id.reference_link_card)
        )
    }

    private fun setUpdateSymptomsDialog() {
        updateSymptomsDialog = BottomSheetDialog(this, R.style.PersistentBottomSheet)
        updateSymptomsDialog.setContentView(
            layoutInflater.inflate(
                R.layout.bottom_sheet_isolate,
                null
            )
        )
        updateSymptomsDialog.behavior.isHideable = false

        updateSymptomsDialog.findViewById<Button>(R.id.no_symptoms)?.setOnClickListener {
            userStateStorage.update(DefaultState())
            navigateTo(userStateStorage.get())
            updateSymptomsDialog.cancel()
        }

        updateSymptomsDialog.setOnCancelListener {
            finish()
        }

        updateSymptomsDialog.findViewById<Button>(R.id.have_symptoms)?.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }
    }

    override fun onResume() {
        super.onResume()

        val state = userStateStorage.get()
        navigateTo(state)

        if (state.hasExpired()) {
            updateSymptomsDialog.showExpanded()
            checkInReminderNotification.hide()
        } else {
            updateSymptomsDialog.dismiss()
        }
    }

    override fun onPause() {
        super.onPause()
        updateSymptomsDialog.dismiss()
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        private fun getIntent(context: Context) =
            Intent(context, IsolateActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
