package uk.nhs.nhsx.sonar.android.app.common.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_error.view.errorDescription
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.isInversionModeEnabled

class ErrorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        initializeViews()
        applyAttributes(context, attrs)
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) handleInversion(context.isInversionModeEnabled())
    }

    private fun handleInversion(inversionModeEnabled: Boolean) {
        if (inversionModeEnabled)
            setBackgroundColor(context.getColor(R.color.white))
        else
            setBackgroundColor(context.getColor(R.color.error_background))
    }

    private fun initializeViews() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_error, this)
    }

    private fun applyAttributes(context: Context, attrs: AttributeSet?) {
        context.theme.obtainStyledAttributes(attrs, R.styleable.ErrorView, 0, 0)
            .apply {
                val description: String? =
                    try {
                        getString(R.styleable.ErrorView_error_description)
                    } finally {
                        recycle()
                    }
                errorDescription.text = description
            }
    }
}
