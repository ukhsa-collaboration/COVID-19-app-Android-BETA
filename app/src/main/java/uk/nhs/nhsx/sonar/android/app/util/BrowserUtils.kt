/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.app.Activity
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import uk.nhs.nhsx.sonar.android.app.R

fun Activity.openUrl(url: String) {
    CustomTabsIntent.Builder()
        .setToolbarColor(getColor(R.color.colorPrimary))
        .build()
        .launchUrl(this, Uri.parse(url))
}

const val LATEST_ADVICE_URL =
    "https://www.gov.uk/government/publications/full-guidance-on-staying-at-home-and-away-from-others/full-guidance-on-staying-at-home-and-away-from-others"

const val LATEST_ADVICE_URL_RED_STATE =
    "https://www.gov.uk/government/publications/covid-19-stay-at-home-guidance/stay-at-home-guidance-for-households-with-possible-coronavirus-covid-19-infection"

const val NHS_SYMPTOM_CHECKER = "https://111.nhs.uk/covid-19/"

const val NHS_SUPPORT_PAGE = "https://www.nhs.uk/conditions/coronavirus-covid-19/"
