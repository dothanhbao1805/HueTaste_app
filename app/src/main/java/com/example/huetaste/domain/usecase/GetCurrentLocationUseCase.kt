package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.repository.LocationProvider
import javax.inject.Inject

class GetCurrentLocationUseCase @Inject constructor(private val provider: LocationProvider) {
    suspend operator fun invoke(): GeoPoint? = provider.current()
}
