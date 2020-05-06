/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.registration_panel.view.registrationProgressBar
import kotlinx.android.synthetic.main.registration_panel.view.registrationStatusIcon
import kotlinx.android.synthetic.main.registration_panel.view.registrationStatusText
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.Complete
import uk.nhs.nhsx.sonar.android.app.status.RegistrationState.InProgress

class RegistrationProgressPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var state: RegistrationState? = null

    init {
        initializeViews()
    }

    private fun initializeViews() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.registration_panel, this)
    }

    fun setState(newState: RegistrationState) {
        if (state != newState) {
            state = newState
            when (state) {
                InProgress -> setInProgressState()
                Complete -> setRegisteredState()
            }
        }
    }

    private fun setInProgressState() {
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        registrationProgressBar.isVisible = true
        registrationStatusIcon.isVisible = false
        registrationStatusText.setText(R.string.registration_finalising_setup)
        registrationStatusText.setTextColor(ContextCompat.getColor(context, R.color.black))
    }

    private fun setRegisteredState() {
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        registrationProgressBar.isVisible = false
        registrationStatusIcon.isVisible = true
        registrationStatusIcon.setImageResource(R.drawable.ic_success_outline)
        registrationStatusText.setText(R.string.registration_everything_is_working_ok)
        registrationStatusText.setTextColor(ContextCompat.getColor(context, R.color.black))
    }
}
