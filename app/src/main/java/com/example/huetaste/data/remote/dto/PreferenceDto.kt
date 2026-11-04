package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `SoThichOut`, `CheDoAnOut`, `NguyenLieuOut` and `NhuCau` schemas. */
data class SoThichOutDto(
    val idSoThich: String,
    val loai: String,
    val tenSoThich: String,
    /** Only present on the user's own list. */
    val mucDo: Int?,
)

data class CheDoAnOutDto(
    val idCheDoAn: String,
    val tenCheDo: String,
    val moTa: String?,
)

data class NguyenLieuOutDto(
    val idNguyenLieu: String,
    val tenNguyenLieu: String,
    val nhomNguyenLieu: String?,
)

data class SoThichNguoiDungBody(
    val idSoThich: String,
    val mucDo: Int,
)

data class CapNhatSoThichBody(val soThich: List<SoThichNguoiDungBody>)

data class CheDoAnNguoiDungBody(val idCheDoAn: String)

data class CapNhatCheDoAnBody(val cheDoAn: List<CheDoAnNguoiDungBody>)

/** `ngay` is left out so the backend uses today in Vietnam time; Gson drops the null fields. */
data class NhuCauBody(
    val buaAn: String,
    val gioAn: String?,
    val nganSach: Long?,
    val nguyenLieuTranh: List<String>,
)
