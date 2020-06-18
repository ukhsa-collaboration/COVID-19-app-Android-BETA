/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.status.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.view_status.view.statusColorBar
import kotlinx.android.synthetic.main.view_status.view.statusDescription
import kotlinx.android.synthetic.main.view_status.view.statusTitle
import uk.nhs.nhsx.sonar.android.app.R

class StatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        initializeViews()
    }

    private fun initializeViews() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.view_status, this)
    }

    fun update(configuration: Configuration) {
        val background = when (configuration.statusColor) {
            Color.BLUE -> R.drawable.initial_card_background
            Color.ORANGE -> R.drawable.symptomatic_card_background
        }
        statusColorBar.setBackgroundResource(background)

        statusTitle.text = configuration.title

        if (configuration.description != null) {
            statusDescription.text = configuration.description
            statusDescription.isVisible = true
        } else {
            statusDescription.isVisible = false
        }
    }

    data class Configuration(
        val title: String,
        val description: CharSequence? = null,
        val statusColor: Color
    )

    enum class Color {
        BLUE, ORANGE
    }
}
