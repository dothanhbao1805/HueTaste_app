package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `ScanOut` schema. */
data class ScanOutDto(
    val idLichSuScan: String,
    val trangThai: String,
    val duongDanAnh: String,
    val tenMonNhanDien: String?,
    val doTinCay: Float?,
    val moTaNgan: String?,
    val laMonDacTrungHue: Boolean?,
    val idMonAn: String?,
    val monAn: MonAnChiTietDto?,
    val monTuongTu: List<MonAnDto>?,
)

data class ErrorBodyDto(
    val code: String?,
    val message: String?,
)
