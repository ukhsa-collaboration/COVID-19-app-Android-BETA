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

private const val PAGE_ADVICE_DEFAULT = "https://faq.covid19.nhs.uk/article/KA-01062/en-us"

private const val PAGE_ADVICE_RED = "https://faq.covid19.nhs.uk/article/KA-01078/en-us"

private const val PAGE_ADVICE_EXPOSED = "https://faq.covid19.nhs.uk/article/KA-01063/en-us"

const val URL_LATEST_ADVICE_DEFAULT =
    "$PAGE_ADVICE_DEFAULT?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_GENERAL"

const val URL_LATEST_ADVICE_RED =
    "$PAGE_ADVICE_RED?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_SYMPTOMS#main-messages"

const val URL_LATEST_ADVICE_EXPOSED =
    "$PAGE_ADVICE_EXPOSED?$UTM_SOURCE&$UTM_MEDIUM&$UTM_CAMPAIGN&$UTM_CONTENT_NOTIFIED"

const val URL_SYMPTOM_CHECKER = "https://111.nhs.uk/covid-19/"

const val URL_INFO = "https://covid19.nhs.uk/"
const val URL_PRIVACY_NOTICE = "https://covid19.nhs.uk/privacy-and-data.html"
const val URL_TERMS_OF_USE = "https://covid19.nhs.uk/our-policies.html"

const val URL_NHS_LOCAL_SUPPORT = "https://faq.covid19.nhs.uk/article/KA-01065/en-us"

const val WORKPLACE_GUIDANCE = "https://faq.covid19.nhs.uk/article/KA-01072/en-us"
