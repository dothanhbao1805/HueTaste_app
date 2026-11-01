package com.example.huetaste.di

import com.example.huetaste.data.repository.AuthRepositoryImpl
import com.example.huetaste.data.repository.DishRepositoryImpl
import com.example.huetaste.data.repository.PlaceRepositoryImpl
import com.example.huetaste.data.repository.ScanRepositoryImpl
import com.example.huetaste.domain.repository.AuthRepository
import com.example.huetaste.domain.repository.DishRepository
import com.example.huetaste.domain.repository.PlaceRepository
import com.example.huetaste.domain.repository.ScanRepository
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
    abstract fun bindScanRepository(impl: ScanRepositoryImpl): ScanRepository

    @Binds
    @Singleton
    abstract fun bindDishRepository(impl: DishRepositoryImpl): DishRepository

    @Binds
    @Singleton
    abstract fun bindPlaceRepository(impl: PlaceRepositoryImpl): PlaceRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
