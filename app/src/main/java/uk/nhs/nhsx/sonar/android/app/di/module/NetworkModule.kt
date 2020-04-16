/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package uk.nhs.nhsx.sonar.android.app.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.client.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.EncryptionKeyStorage
import uk.nhs.nhsx.sonar.android.client.ResidentApi
import uk.nhs.nhsx.sonar.android.client.http.HttpClient

@Module
class NetworkModule(private val baseUrl: String) {

    @Provides
    fun provideHttpClient(context: Context): HttpClient =
        HttpClient(context)

    @Provides
    fun residentApi(encryptionKeyStorage: EncryptionKeyStorage, httpClient: HttpClient): ResidentApi =
        ResidentApi(baseUrl, encryptionKeyStorage, httpClient)

    @Provides
    fun coLocationApi(encryptionKeyStorage: EncryptionKeyStorage, httpClient: HttpClient): CoLocationApi =
        CoLocationApi(baseUrl, encryptionKeyStorage, httpClient)
}
