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

private const val PAGE_ADVICE_DEFAULT = "https://faq.covid19.nhs.uk/article/KA-01062/en-us"

private const val PAGE_ADVICE_SYMPTOMATIC = "https://faq.covid19.nhs.uk/article/KA-01078/en-us"

private const val PAGE_ADVICE_POSITIVE = "https://faq.covid19.nhs.uk/article/KA-01064/en-us"

private const val PAGE_ADVICE_EXPOSED = "https://faq.covid19.nhs.uk/article/KA-01063/en-us"

const val URL_LATEST_ADVICE_DEFAULT = PAGE_ADVICE_DEFAULT

const val URL_LATEST_ADVICE_SYMPTOMATIC = PAGE_ADVICE_SYMPTOMATIC

const val URL_LATEST_ADVICE_POSITIVE = PAGE_ADVICE_POSITIVE

const val URL_LATEST_ADVICE_EXPOSED = PAGE_ADVICE_EXPOSED

const val URL_SYMPTOM_CHECKER = "https://111.nhs.uk/covid-19/"

const val URL_INFO = "https://covid19.nhs.uk/"
const val URL_PRIVACY_NOTICE = "https://covid19.nhs.uk/privacy-and-data.html"
const val URL_TERMS_OF_USE = "https://covid19.nhs.uk/our-policies.html"

const val URL_NHS_LOCAL_SUPPORT = "https://faq.covid19.nhs.uk/article/KA-01065/en-us"
const val URL_NHS_NOT_SUPPORTED_DEVICE = "https://faq.covid19.nhs.uk/article/KA-01073/en-us"
const val URL_NHS_TABLET_DEVICE = "https://faq.covid19.nhs.uk/article/KA-01079/en-us"

const val WORKPLACE_GUIDANCE = "https://faq.covid19.nhs.uk/article/KA-01072/en-us"
