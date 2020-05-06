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
        .addDefaultShareMenuItem()
        .setToolbarColor(getColor(R.color.colorPrimary))
        .build()
        .launchUrl(this, Uri.parse(url))
}

private const val UTM_SOURCE = "utm_source=nhscovid19android"
private const val UTM_MEDIUM = "utm_medium=mobileapp"
private const val UTM_CAMPAIGN = "utm_campaign=nhscovid19app"
private const val UTM_CONTENT_GENERAL = "utm_content=general"
private const val UTM_CONTENT_SYMPTOMS = "utm_content=symptoms"
private const val UTM_CONTENT_NOTIFIED = "utm_content=notified"

private const val PAGE_NHS_CONDITIONS = "https://www.nhs.uk/conditions/coronavirus-covid-19"

private const val PAGE_ADVICE_DEFAULT =
    "https://www.gov.uk/government/publications/full-guidance-on-staying-at-home-and-away-from-others/full-guidance-on-staying-at-home-and-away-from-others"

private const val PAGE_ADVICE_RED =
    "https://www.gov.uk/government/publications/covid-19-stay-at-home-guidance/stay-at-home-guidance-for-households-with-possible-coronavirus-covid-19-infection"

private const val PAGE_ADVICE_AMBER = "https://www.gov.uk/coronavirus"

const val URL_SUPPORT_DEFAULT = "$PAGE_NHS_CONDITIONS?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_GENERAL"
const val URL_SUPPORT_RED = "$PAGE_NHS_CONDITIONS?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_SYMPTOMS"
const val URL_SUPPORT_AMBER = "$PAGE_NHS_CONDITIONS?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_NOTIFIED"

const val URL_LATEST_ADVICE_DEFAULT = "$PAGE_ADVICE_DEFAULT?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_GENERAL"

const val URL_LATEST_ADVICE_RED =
    "$PAGE_ADVICE_RED?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_SYMPTOMS#main-messages"

const val URL_LATEST_ADVICE_AMBER =
    "$PAGE_ADVICE_AMBER?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_NOTIFIED"

const val URL_SYMPTOM_CHECKER = "https://111.nhs.uk/covid-19/"

const val URL_INFO = "http://covid19.nhs.uk/"
const val URL_PRIVACY_NOTICE = "https://covid19.nhs.uk/privacy-and-data.html"
const val URL_TERMS_OF_USE = "https://covid19.nhs.uk/our-policies.html"
