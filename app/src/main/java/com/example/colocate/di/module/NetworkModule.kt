/*
 * Copyright © 2020 NHSX. All rights reserved.
 */

package com.example.colocate.di.module

import android.content.Context
import dagger.Module
import dagger.Provides
import uk.nhs.nhsx.sonar.android.client.http.HttpClient
import uk.nhs.nhsx.sonar.android.client.http.volley.VolleyHttpClient

@Module
class NetworkModule(private val baseUrl: String) {
    @Provides
    fun provideHttpClient(context: Context): HttpClient = VolleyHttpClient(baseUrl, context)
}
