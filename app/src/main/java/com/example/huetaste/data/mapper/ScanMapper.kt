package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.LichSuScanOutDto
import com.example.huetaste.data.remote.dto.ScanOutDto
import com.example.huetaste.data.remote.dto.TrangLichSuScanDto
import com.example.huetaste.domain.model.ScanHistoryItem
import com.example.huetaste.domain.model.ScanHistoryPage
import com.example.huetaste.domain.model.ScanResult
import com.example.huetaste.domain.model.ScanStatus

private fun scanStatusOf(value: String?) = when (value) {
    "CO_TRONG_HE_THONG" -> ScanStatus.IN_CATALOG
    "DANG_CAP_NHAT" -> ScanStatus.UPDATING
    "CHUA_XAC_MINH" -> ScanStatus.UNVERIFIED
    else -> ScanStatus.NOT_SAVED
}

fun TrangLichSuScanDto.toDomain(requestedPage: Int) = ScanHistoryPage(
    items = items.orEmpty().map { it.toDomain() },
    total = total ?: 0,
    page = page ?: requestedPage,
)

fun LichSuScanOutDto.toDomain() = ScanHistoryItem(
    id = idLichSuScan,
    status = scanStatusOf(trangThai),
    imageUrl = duongDanAnh,
    name = tenMonNhanDien.orEmpty(),
    confidence = doTinCay,
    description = moTaNgan?.takeIf { it.isNotBlank() },
    isHueSpecialty = laMonDacTrungHue == true,
    isSaved = daLuu == true,
    dishId = idMonAn,
    createdAt = parseBackendTime(ngayTao),
)

fun ScanOutDto.toDomain() = ScanResult(
    id = idLichSuScan,
    status = scanStatusOf(trangThai),
    imageUrl = duongDanAnh,
    recognizedName = tenMonNhanDien ?: monAn?.tenMon.orEmpty(),
    confidence = doTinCay,
    shortDescription = moTaNgan,
    isHueSpecialty = laMonDacTrungHue == true,
    dishId = idMonAn,
    dish = monAn?.toDomain(),
    similarDishes = monTuongTu.orEmpty().map { it.toDomain() },
)
