package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.ThongBaoDto
import com.example.huetaste.data.remote.dto.TrangThongBaoDto
import com.example.huetaste.domain.model.AppNotification
import com.example.huetaste.domain.model.NotificationPage
import com.example.huetaste.domain.model.NotificationType

fun ThongBaoDto.toDomain() = AppNotification(
    id = idThongBao,
    type = when (loai) {
        "BAO_CAO" -> NotificationType.REPORT
        "MON_SCAN" -> NotificationType.SCANNED_DISH
        "NHAC_BUA_AN" -> NotificationType.MEAL_REMINDER
        "NGAY_RAM" -> NotificationType.LUNAR_DAY
        "THOI_TIET" -> NotificationType.WEATHER
        // A kind added to the backend later still shows, it just opens nothing.
        else -> NotificationType.OTHER
    },
    title = tieuDe.orEmpty(),
    body = noiDung.orEmpty(),
    dishId = idMonAn,
    placeId = idDiaDiem,
    reportId = idBaoCao,
    isRead = daDoc ?: false,
    createdAt = ngayTao,
)

fun TrangThongBaoDto.toDomain() = NotificationPage(
    items = items.orEmpty().map { it.toDomain() },
    total = total ?: 0,
    page = page ?: 1,
)
