/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_ok.notificationPanel
import kotlinx.android.synthetic.main.activity_status.bookTest
import kotlinx.android.synthetic.main.activity_status.feelUnwell
import kotlinx.android.synthetic.main.activity_status.feelUnwellSubtitle
import kotlinx.android.synthetic.main.activity_status.readLatestAdvice
import kotlinx.android.synthetic.main.activity_status.registrationPanel
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.nhsServiceFooter
import kotlinx.android.synthetic.main.status_footer_view.reference_link_card
import kotlinx.android.synthetic.main.status_footer_view.workplace_guidance_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.interstitials.CurrentAdviceActivity
import uk.nhs.nhsx.sonar.android.app.interstitials.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.notifications.CheckInReminderNotification
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeActivity
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusScreen
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusScreenFactory
import uk.nhs.nhsx.sonar.android.app.status.widgets.createTestResultDialog
import uk.nhs.nhsx.sonar.android.app.status.widgets.handleTestResult
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_NHS_LOCAL_SUPPORT
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
import uk.nhs.nhsx.sonar.android.app.util.openAppSettings
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class StatusActivity : BaseActivity() {

    internal lateinit var statusScreen: StatusScreen

    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    protected lateinit var userInbox: UserInbox

    @Inject
    lateinit var checkInReminderNotification: CheckInReminderNotification

    internal lateinit var updateSymptomsDialog: BottomSheetDialog
    private lateinit var testResultDialog: BottomDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)
        BluetoothService.start(this)

        registrationPanel.setState(RegistrationState.Complete)

        hideNotSharedWidgets()
        statusScreen = StatusScreenFactory.from(userStateStorage.get())
        statusScreen.setStatusScreen(this)

        readLatestAdvice.setOnClickListener {
            CurrentAdviceActivity.start(this)
        }

        feelUnwell.setOnClickListener {
            DiagnoseTemperatureActivity.start(this)
        }

        nhsServiceFooter.setOnClickListener {
            openUrl(URL_NHS_LOCAL_SUPPORT)
        }

        toolbar_info.setOnClickListener {
            openUrl(URL_INFO)
        }

        workplace_guidance_card.setOnClickListener {
            WorkplaceGuidanceActivity.start(this)
        }

        reference_link_card.setOnClickListener {
            ReferenceCodeActivity.start(this)
        }

        notificationPanel.setOnClickListener {
            openAppSettings()
        }

        setUpdateSymptomsDialog()
        testResultDialog = createTestResultDialog(this, userInbox)
    }

    private fun setUpdateSymptomsDialog() {
        val configuration = BottomDialogConfiguration(
            isHideable = false,
            titleResId = R.string.status_today_feeling,
            textResId = R.string.update_symptoms_prompt,
            firstCtaResId = R.string.update_my_symptoms,
            secondCtaResId = R.string.no_symptoms
        )
        updateSymptomsDialog = BottomDialog(
            this, configuration,
            onCancel = {
                finish()
            },
            onFirstCtaClick = {
                DiagnoseTemperatureActivity.start(this)
            },
            onSecondCtaClick = {
                userStateStorage.set(DefaultState)
                navigateTo(userStateStorage.get())
            }
        )
    }

    override fun onResume() {
        super.onResume()

        val state = userStateStorage.get()
        navigateTo(state)

        statusScreen.onResume(this)

        notificationPanel.isVisible =
            !NotificationManagerCompat.from(this).areNotificationsEnabled()

        handleTestResult(userInbox, testResultDialog)
    }

    override fun handleInversion(inversionModeEnabled: Boolean) {
        notificationPanel.cardColourInversion(inversionModeEnabled)

        readLatestAdvice.cardColourInversion(inversionModeEnabled)
        bookTest.cardColourInversion(inversionModeEnabled)
        feelUnwell.cardColourInversion(inversionModeEnabled)

        workplace_guidance_card.cardColourInversion(inversionModeEnabled)
        reference_link_card.cardColourInversion(inversionModeEnabled)
    }

    override fun onPause() {
        super.onPause()
        updateSymptomsDialog.dismiss()
    }

    private fun hideNotSharedWidgets() {
        bookTest.isVisible = false
        feelUnwell.isVisible = false
        feelUnwellSubtitle.isVisible = false
    }

    companion object {
        fun start(context: Context) =
            context.startActivity(getIntent(context))

        fun getIntent(context: Context) =
            Intent(context, StatusActivity::class.java)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
    }
}
