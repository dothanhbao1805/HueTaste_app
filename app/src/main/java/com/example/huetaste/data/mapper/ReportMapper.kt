package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.BaoCaoDto
import com.example.huetaste.data.remote.dto.ChiTietBaoCaoDto
import com.example.huetaste.data.remote.dto.TrangBaoCaoDto
import com.example.huetaste.domain.model.PlaceReport
import com.example.huetaste.domain.model.ReportDishResult
import com.example.huetaste.domain.model.ReportPage
import com.example.huetaste.domain.model.ReportStatus
import com.example.huetaste.domain.model.ReportedDish

fun BaoCaoDto.toDomain() = PlaceReport(
    id = idBaoCao,
    placeId = diaDiem?.idDiaDiem,
    placeName = diaDiem?.tenDiaDiem,
    placeHidden = diaDiem?.trangThai == "hidden",
    reason = lyDo.orEmpty(),
    description = moTa?.takeIf { it.isNotBlank() },
    // Anything but a handled report is still waiting for the admins.
    status = if (trangThai == "DA_XU_LY") ReportStatus.RESOLVED else ReportStatus.PENDING,
    resolutionNote = ketQuaXuLy?.takeIf { it.isNotBlank() },
    resolvedAt = xuLyLuc,
    createdAt = ngayTao,
    dishes = chiTiet.orEmpty().mapNotNull { it.toDomain() },
)

private fun ChiTietBaoCaoDto.toDomain(): ReportedDish? {
    val dish = monAn ?: return null
    return ReportedDish(
        dishId = dish.idMonAn,
        dishName = dish.tenMon,
        result = when (ketQuaXuLy) {
            "VI_PHAM" -> ReportDishResult.VIOLATION
            "KHONG_VI_PHAM" -> ReportDishResult.NO_VIOLATION
            else -> ReportDishResult.PENDING
        },
    )
}

fun TrangBaoCaoDto.toDomain() = ReportPage(
    reports = items.orEmpty().map { it.toDomain() },
    total = total ?: 0,
    page = page ?: 1,
)
