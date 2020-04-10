/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.client.colocation.CoLocationApi
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient
import uk.nhs.nhsx.sonar.android.client.resident.ResidentApi
import uk.nhs.nhsx.sonar.android.client.security.EncryptionKeyStorage

@Module
class NetworkModule(private val baseUrl: String) {

    @Provides
    fun provideHttpClient(context: Context): HttpClient =
        VolleyHttpClient(context)

    @Provides
    fun residentApi(encryptionKeyStorage: EncryptionKeyStorage, httpClient: HttpClient): ResidentApi =
        ResidentApi(baseUrl, encryptionKeyStorage, httpClient)

    @Provides
    fun coLocationApi(encryptionKeyStorage: EncryptionKeyStorage, httpClient: HttpClient): CoLocationApi =
        CoLocationApi(baseUrl, encryptionKeyStorage, httpClient)
}
