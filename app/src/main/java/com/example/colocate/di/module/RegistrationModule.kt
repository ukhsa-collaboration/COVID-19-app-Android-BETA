package com.example.colocate.di.module

import com.example.colocate.registration.FirebaseTokenRetriever
import com.example.colocate.registration.TokenRetriever
import dagger.Module
import dagger.Provides

@Module
class RegistrationModule {

    @Provides
    fun provideTokenRetriever(): TokenRetriever {
        return FirebaseTokenRetriever()
    }
}
