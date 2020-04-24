/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.app.diagnose.review.CoLocationApi
import uk.nhs.nhsx.sonar.android.app.http.HttpClient
import uk.nhs.nhsx.sonar.android.app.http.KeyStorage
import uk.nhs.nhsx.sonar.android.app.registration.ResidentApi

@Module
class NetworkModule(
    private val baseUrl: String,
    private val sonarHeaderValue: String
) {

    @Provides
    fun provideHttpClient(context: Context): HttpClient =
        HttpClient(context, sonarHeaderValue)

    @Provides
    fun residentApi(keyStorage: KeyStorage, httpClient: HttpClient): ResidentApi =
        ResidentApi(baseUrl, keyStorage, httpClient)

    @Provides
    fun coLocationApi(keyStorage: KeyStorage, httpClient: HttpClient): CoLocationApi =
        CoLocationApi(baseUrl, keyStorage, httpClient)
}
