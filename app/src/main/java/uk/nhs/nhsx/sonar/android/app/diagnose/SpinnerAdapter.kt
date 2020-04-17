package uk.nhs.nhsx.sonar.android.app.diagnose

import android.content.Context
import android.widget.ArrayAdapter
import org.joda.time.DateTime
import uk.nhs.nhsx.sonar.android.app.R
import uk.nhs.nhsx.sonar.android.app.util.toUiSpinnerFormat

class SpinnerAdapter(context: Context) :
    ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, getLastSevenDays(context))

private fun getLastSevenDays(context: Context): List<String> =
    mutableListOf<String>().apply {
        add(context.getString(R.string.today))
        add(context.getString(R.string.yesterday))
        (2..7).forEach {
            add(DateTime.now().minusDays(it).toUiSpinnerFormat())
        }
    }.toList()
