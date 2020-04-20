/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose.review

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import org.joda.time.LocalDate
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.toUiSpinnerFormat

class SpinnerAdapter(context: Context) :
    ArrayAdapter<String>(context, android.R.layout.simple_spinner_item,
        getLastSevenDays(
            context
        )
    ) {

    init {
        setDropDownViewResource(R.layout.item_date_spinner)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
        super.getDropDownView(position, convertView, parent)
            .apply {
                if (position == count - 1) background = null
            }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val v: View = super.getView(position, convertView, parent)

        if (position == count) {
            (v.findViewById(android.R.id.text1) as TextView).text =
                context.getString(R.string.start_date)
            (v.findViewById(android.R.id.text1) as TextView).hint = getItem(count)
        }
        return v
    }

    override fun getCount(): Int = super.getCount() - 1

    companion object {
        private fun getLastSevenDays(context: Context): List<String> {
            val today = LocalDate.now()

            return listOf(
                context.getString(R.string.today),
                context.getString(R.string.yesterday),
                today.minusDays(2).toUiSpinnerFormat(),
                today.minusDays(3).toUiSpinnerFormat(),
                today.minusDays(4).toUiSpinnerFormat(),
                today.minusDays(5).toUiSpinnerFormat(),
                today.minusDays(6).toUiSpinnerFormat(),
                today.minusDays(7).toUiSpinnerFormat(),
                context.getString(R.string.before_day),
                context.getString(R.string.start_date)
            )
        }
    }
}
