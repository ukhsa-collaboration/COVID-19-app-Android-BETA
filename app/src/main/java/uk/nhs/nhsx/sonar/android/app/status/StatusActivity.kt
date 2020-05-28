/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_status.bookTest
import kotlinx.android.synthetic.main.activity_status.feelUnwell
import kotlinx.android.synthetic.main.activity_status.nextStepsAdvice
import kotlinx.android.synthetic.main.activity_status.notificationPanel
import kotlinx.android.synthetic.main.activity_status.readLatestAdvice
import kotlinx.android.synthetic.main.activity_status.registrationPanel
import kotlinx.android.synthetic.main.banner.toolbar_info
import kotlinx.android.synthetic.main.status_footer_view.nhsServiceFooter
import kotlinx.android.synthetic.main.status_footer_view.reference_link_card
import kotlinx.android.synthetic.main.status_footer_view.workplace_guidance_card
import uk.nhs.nhsx.sonar.android.app.BaseActivity
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.ViewModelFactory
import uk.nhs.nhsx.sonar.android.app.appComponent
import uk.nhs.nhsx.sonar.android.app.ble.BluetoothService
import uk.nhs.nhsx.sonar.android.app.diagnose.DiagnoseTemperatureActivity
import uk.nhs.nhsx.sonar.android.app.inbox.UserInbox
import uk.nhs.nhsx.sonar.android.app.interstitials.CurrentAdviceActivity
import uk.nhs.nhsx.sonar.android.app.interstitials.WorkplaceGuidanceActivity
import uk.nhs.nhsx.sonar.android.app.notifications.CheckInReminderNotification
import uk.nhs.nhsx.sonar.android.app.referencecode.ReferenceCodeActivity
import uk.nhs.nhsx.sonar.android.app.registration.SonarIdProvider
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusScreen
import uk.nhs.nhsx.sonar.android.app.status.widgets.StatusScreenFactory
import uk.nhs.nhsx.sonar.android.app.status.widgets.createTestResultDialog
import uk.nhs.nhsx.sonar.android.app.status.widgets.handleTestResult
import uk.nhs.nhsx.sonar.android.app.status.widgets.toggleNotFeelingCard
import uk.nhs.nhsx.sonar.android.app.status.widgets.toggleReferenceCodeCard
import uk.nhs.nhsx.sonar.android.app.util.URL_INFO
import uk.nhs.nhsx.sonar.android.app.util.URL_NHS_LOCAL_SUPPORT
import uk.nhs.nhsx.sonar.android.app.util.cardColourInversion
import uk.nhs.nhsx.sonar.android.app.util.observe
import uk.nhs.nhsx.sonar.android.app.util.openAppSettings
import uk.nhs.nhsx.sonar.android.app.util.openUrl
import javax.inject.Inject

class StatusActivity : BaseActivity() {

    internal lateinit var statusScreen: StatusScreen

    @Inject
    lateinit var userStateStorage: UserStateStorage

    @Inject
    lateinit var userInbox: UserInbox

    @Inject
    lateinit var checkInReminderNotification: CheckInReminderNotification

    @Inject
    lateinit var sonarIdProvider: SonarIdProvider

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<OkViewModel>
    private val viewModel: OkViewModel by viewModels { viewModelFactory }

    lateinit var recoveryDialog: BottomDialog
    internal lateinit var updateSymptomsDialog: BottomSheetDialog
    private lateinit var testResultDialog: BottomDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_status)

        if (sonarIdProvider.hasProperSonarId()) {
            BluetoothService.start(this)
        }

        registrationPanel.setState(RegistrationState.Complete)

        hideNotSharedWidgets()
        val userState = userStateStorage.get()
        statusScreen = StatusScreenFactory.from(userState)
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

        recoveryDialog = createRecoveryDialog()
        updateSymptomsDialog = createUpdateSymptomsDialog()
        testResultDialog = createTestResultDialog(this, userInbox)

        // TODO: maybe move this check into view model?
        if (userState is DefaultState) {
            toggleReferenceCodeCard(this, false)
            toggleNotFeelingCard(this, false)
            addViewModelListener()
            viewModel.onStart()
        }
    }

    /**
     * Note: this is only active for DefaultState
     */
    private fun addViewModelListener() {
        viewModel.viewState().observe(this) { result ->
            when (result) {
                RegistrationState.Complete -> {
                    registrationPanel.setState(result)
                    BluetoothService.start(this)
                    toggleNotFeelingCard(this, true)
                    toggleReferenceCodeCard(this, true)
                }
                RegistrationState.InProgress -> {
                    registrationPanel.setState(result)
                    toggleNotFeelingCard(this, false)
                    toggleReferenceCodeCard(this, false)
                }
            }
        }
    }

    private fun createRecoveryDialog(): BottomDialog {
        val configuration = BottomDialogConfiguration(
            isHideable = false,
            titleResId = R.string.recovery_dialog_title,
            textResId = R.string.recovery_dialog_description,
            secondCtaResId = R.string.okay
        )
        return BottomDialog(this, configuration,
            onCancel = {
                userInbox.dismissRecovery()
                finish()
            },
            onSecondCtaClick = {
                userInbox.dismissRecovery()
            })
    }

    private fun createUpdateSymptomsDialog(): BottomDialog {
        val configuration = BottomDialogConfiguration(
            isHideable = false,
            titleResId = R.string.status_today_feeling,
            textResId = R.string.update_symptoms_prompt,
            firstCtaResId = R.string.update_my_symptoms,
            secondCtaResId = R.string.no_symptoms
        )
        return BottomDialog(
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
        recoveryDialog.dismiss()
    }

    private fun hideNotSharedWidgets() {
        bookTest.isVisible = false
        feelUnwell.isVisible = false
        nextStepsAdvice.isVisible = false
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
