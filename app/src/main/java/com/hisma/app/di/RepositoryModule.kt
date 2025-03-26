package com.hisma.app.di

import com.hisma.app.data.repository.AuthRepositoryImpl
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.data.repository.LubricenterRepositoryImpl
import com.hisma.app.data.repository.SubscriptionRepositoryImpl
import com.hisma.app.domain.repository.LubricenterRepository
import com.hisma.app.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLubricenterRepository(
        lubricenterRepositoryImpl: LubricenterRepositoryImpl
    ): LubricenterRepository

    // Agrega este método para proporcionar SubscriptionRepository
    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        subscriptionRepositoryImpl: SubscriptionRepositoryImpl
    ): SubscriptionRepository
}