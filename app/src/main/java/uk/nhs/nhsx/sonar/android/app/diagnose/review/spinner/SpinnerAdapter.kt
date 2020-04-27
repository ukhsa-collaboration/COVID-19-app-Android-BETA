/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.diagnose.review.spinner

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.toUiSpinnerFormat

class SpinnerAdapter(context: Context) :
    ArrayAdapter<String>(
        context, android.R.layout.simple_spinner_item,
        getDates(
            context
        )
    ) {

    init {
        setDropDownViewResource(R.layout.item_date_spinner)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return if (isVisibleItem(position)) {
            super.getDropDownView(position, convertView, parent)
        } else {
            View(context).apply {
                visibility = View.GONE
            }
        }
    }

    private fun isVisibleItem(position: Int) = position <= MAX_VISIBLE_POSITION

    fun update(data: String) {
        clear()
        addAll(
            getDates(
                context
            )
        )
        insert(data, count - 1)
    }

    companion object {
        const val MAX_VISIBLE_POSITION = 3

        private fun getDates(context: Context): MutableList<String> {
            val today = DateTime.now().toLocalDate()

            return mutableListOf(
                context.getString(R.string.today),
                context.getString(R.string.yesterday),
                today.minusDays(2).toUiSpinnerFormat(),
                context.getString(R.string.other_date),
                context.getString(R.string.start_date)
            )
        }
    }
}
