/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.onboarding

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.sonar.android.app.analytics.SonarAnalytics
import uk.nhs.nhsx.sonar.android.app.analytics.partialPostcodeProvided
import uk.nhs.nhsx.sonar.android.app.util.LiveDataEvent
import java.util.Locale
import javax.inject.Inject

class PostCodeViewModel @Inject constructor(
    private val postCodeProvider: PostCodeProvider,
    private val analytics: SonarAnalytics
) : ViewModel() {

    private val viewState = MutableLiveData<PostCodeViewState>()
    fun viewState(): LiveData<PostCodeViewState> =
        viewState

    private val navigation = MutableLiveData<LiveDataEvent<PostCodeNavigation>>()
    fun navigation(): LiveData<LiveDataEvent<PostCodeNavigation>> =
        navigation

    fun onContinue(postCode: String) {
        val postCodeUpperCased = postCode.toUpperCase(Locale.UK)

        if (postCodeRegex.matches(postCodeUpperCased)) {
            postCodeProvider.setPostCode(postCodeUpperCased)
            analytics.trackEvent(partialPostcodeProvided())
            viewState.value = PostCodeViewState.Valid
            navigation.value = LiveDataEvent(PostCodeNavigation.Permissions)
        } else {
            viewState.value = PostCodeViewState.Invalid
        }
    }

    companion object {
        val postCodeRegex = Regex("^[A-Z]{1,2}[0-9R][0-9A-Z]?")
    }
}
