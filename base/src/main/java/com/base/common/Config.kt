package com.base.common

import android.content.Context
import com.base.common.base.LsApplication
import com.base.common.extension.getDeviceType
import com.base.domain.model.DeviceType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Config @Inject constructor(context: Context) {

    var deviceType: DeviceType = detectBaseDeviceType(context)
        private set

    private fun detectBaseDeviceType(context: Context): DeviceType {
        val configuration = context.resources.configuration
        return if (configuration.smallestScreenWidthDp >= 600) {
            DeviceType.TABLET
        } else {
            DeviceType.PHONE
        }
    }

}