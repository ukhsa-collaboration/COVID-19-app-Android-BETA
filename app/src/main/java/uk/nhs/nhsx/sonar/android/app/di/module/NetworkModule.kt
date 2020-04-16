/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.client.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.KeyStorage
import uk.nhs.nhsx.sonar.android.client.ResidentApi
import uk.nhs.nhsx.sonar.android.client.http.HttpClient

@Module
class NetworkModule(private val baseUrl: String) {

    @Provides
    fun provideHttpClient(context: Context): HttpClient =
        HttpClient(context)

    @Provides
    fun residentApi(keyStorage: KeyStorage, httpClient: HttpClient): ResidentApi =
        ResidentApi(baseUrl, keyStorage, httpClient)

    @Provides
    fun coLocationApi(keyStorage: KeyStorage, httpClient: HttpClient): CoLocationApi =
        CoLocationApi(baseUrl, keyStorage, httpClient)
}
