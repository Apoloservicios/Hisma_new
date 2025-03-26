package com.hisma.app.di

import com.hisma.app.data.repository.AuthRepositoryImpl
import com.hisma.app.data.repository.LubricenterRepositoryImpl
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.data.repository.LubricenterRepository
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
}