package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.ScanOutDto
import com.example.huetaste.domain.model.ScanResult
import com.example.huetaste.domain.model.ScanStatus

fun ScanOutDto.toDomain() = ScanResult(
    id = idLichSuScan,
    status = when (trangThai) {
        "CO_TRONG_HE_THONG" -> ScanStatus.IN_CATALOG
        "DANG_CAP_NHAT" -> ScanStatus.UPDATING
        "CHUA_XAC_MINH" -> ScanStatus.UNVERIFIED
        else -> ScanStatus.NOT_SAVED
    },
    imageUrl = duongDanAnh,
    recognizedName = tenMonNhanDien ?: monAn?.tenMon.orEmpty(),
    confidence = doTinCay,
    shortDescription = moTaNgan,
    isHueSpecialty = laMonDacTrungHue == true,
    dishId = idMonAn,
    dish = monAn?.toDomain(),
    similarDishes = monTuongTu.orEmpty().map { it.toDomain() },
)
