package uk.nhs.nhsx.sonar.android.app.status

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.registration_panel.view.registrationPanelDivider
import kotlinx.android.synthetic.main.registration_panel.view.registrationProgressBar
import kotlinx.android.synthetic.main.registration_panel.view.registrationRetryButton
import kotlinx.android.synthetic.main.registration_panel.view.registrationStatusIcon
import kotlinx.android.synthetic.main.registration_panel.view.registrationStatusText
import uk.nhs.nhsx.sonar.android.app.R

class RegistrationProgressPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    enum class State {
        IN_PROGRESS, FAILED, REGISTERED
    }

    private var retryListener: (() -> Unit)? = null
    private var state = State.IN_PROGRESS

    init {
        initializeViews()
    }

    private fun initializeViews() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.registration_panel, this)
        registrationRetryButton.setOnClickListener {
            retryListener?.invoke()
        }
    }

    fun setRetryListener(listener: () -> Unit) {
        this.retryListener = listener
    }

    fun setState(newState: State) {
        if (state != newState) {
            state = newState
            when (state) {
                State.IN_PROGRESS -> setInProgressState()
                State.FAILED -> setFailedState()
                State.REGISTERED -> setRegisteredState()
            }
        }
    }

    private fun setInProgressState() {
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        registrationRetryButton.isVisible = false
        registrationProgressBar.isVisible = true
        registrationProgressBar.isIndeterminate = false
        registrationStatusIcon.isVisible = false
        registrationPanelDivider.isVisible = true
        registrationStatusText.setText(R.string.registration_finalising_setup)
        registrationStatusText.setTextColor(ContextCompat.getColor(context, R.color.black))
    }

    private fun setRegisteredState() {
        setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        registrationRetryButton.isVisible = false
        registrationProgressBar.isVisible = false
        registrationStatusIcon.isVisible = true
        registrationStatusIcon.setImageResource(R.drawable.ic_success_outline)
        registrationPanelDivider.isVisible = true
        registrationStatusText.setText(R.string.registration_everything_is_working_ok)
        registrationStatusText.setTextColor(ContextCompat.getColor(context, R.color.black))
    }

    private fun setFailedState() {
        setBackgroundColor(ContextCompat.getColor(context, R.color.black))
        registrationRetryButton.isVisible = true
        registrationProgressBar.isVisible = false
        registrationStatusIcon.isVisible = true
        registrationStatusIcon.setImageResource(R.drawable.ic_warning_outline)
        registrationPanelDivider.isVisible = false
        registrationStatusText.setText(R.string.registration_app_setup_failed)
        registrationStatusText.setTextColor(ContextCompat.getColor(context, R.color.white))
    }
}
