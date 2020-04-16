package uk.nhs.nhsx.sonar.android.app.util

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import uk.nhs.nhsx.sonar.android.app.R

fun Activity.openUrl(url: String) {
    CustomTabsIntent.Builder()
        .setToolbarColor(getColor(R.color.colorPrimary))
        .build()
        .launchUrl(
            this,
            Uri.parse(
                url
            )
        )
}

const val LATEST_ADVISE_URL =
    "https://www.gov.uk/government/publications/full-guidance-on-staying-at-home-and-away-from-others/full-guidance-on-staying-at-home-and-away-from-others"

const val LATEST_ADVISE_URL_RED_STATE =
    "https://www.gov.uk/government/publications/covid-19-stay-at-home-guidance/stay-at-home-guidance-for-households-with-possible-coronavirus-covid-19-infection"

const val NSH_SUPPORT_PAGE = "https://111.nhs.uk/covid-19/"
