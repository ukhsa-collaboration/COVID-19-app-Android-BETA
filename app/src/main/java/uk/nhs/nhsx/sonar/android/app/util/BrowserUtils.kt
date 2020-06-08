/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import timber.log.Timber
import uk.nhs.nhsx.sonar.android.app.R

fun Activity.openUrl(url: String, useInternalBrowser: Boolean = true) {
    try {
        if (useInternalBrowser) openInInternalBrowser(url)
        else openInExternalBrowser(url)
    } catch (t: Throwable) {
        Timber.e(t, "Error opening url")
    }
}

private fun Activity.openInInternalBrowser(url: String) {
    CustomTabsIntent.Builder()
        .addDefaultShareMenuItem()
        .setToolbarColor(getColor(R.color.colorPrimary))
        .build()
        .launchUrl(this, Uri.parse(url))
}

private fun Activity.openInExternalBrowser(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

const val URL_LATEST_ADVICE_DEFAULT = "https://faq.covid19.nhs.uk/article/KA-01062/en-us"

const val URL_LATEST_ADVICE_SYMPTOMATIC = "https://faq.covid19.nhs.uk/article/KA-01078/en-us"

const val URL_ADVICE_EXPOSED_SYMPTOMATIC = "https://faq.covid19.nhs.uk/article/KA-01088/en-us"

const val URL_LATEST_ADVICE_EXPOSED = "https://faq.covid19.nhs.uk/article/KA-01063/en-us"

const val URL_LATEST_ADVICE_POSITIVE = "https://faq.covid19.nhs.uk/article/KA-01064/en-us"

const val URL_SYMPTOM_CHECKER = "https://111.nhs.uk/covid-19/"

const val URL_INFO = "https://covid19.nhs.uk/"
const val URL_PRIVACY_NOTICE = "https://covid19.nhs.uk/privacy-and-data.html"
const val URL_TERMS_OF_USE = "https://covid19.nhs.uk/our-policies.html"

const val URL_NHS_LOCAL_SUPPORT = "https://faq.covid19.nhs.uk/article/KA-01065/en-us"
const val URL_NHS_NOT_SUPPORTED_DEVICE = "https://faq.covid19.nhs.uk/article/KA-01073/en-us"
const val URL_NHS_TABLET_DEVICE = "https://faq.covid19.nhs.uk/article/KA-01079/en-us"

const val WORKPLACE_GUIDANCE = "https://faq.covid19.nhs.uk/article/KA-01072/en-us"
