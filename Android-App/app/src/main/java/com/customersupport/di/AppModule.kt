package com.customersupport.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // All dependencies are provided via @Inject constructors
    // This module is here for future expansion if needed
}
