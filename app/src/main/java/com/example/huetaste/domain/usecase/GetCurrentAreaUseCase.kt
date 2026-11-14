package com.example.huetaste.domain.usecase

import com.example.huetaste.domain.model.DeviceArea
import com.example.huetaste.domain.repository.LocationProvider
import javax.inject.Inject

class GetCurrentAreaUseCase @Inject constructor(private val provider: LocationProvider) {
    suspend operator fun invoke(): DeviceArea = provider.currentArea()
}
