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

/** Mirrors the backend `LichSuScanOut`. */
data class LichSuScanOutDto(
    val idLichSuScan: String,
    val trangThai: String?,
    val duongDanAnh: String,
    val tenMonNhanDien: String?,
    val doTinCay: Float?,
    val moTaNgan: String?,
    val laMonDacTrungHue: Boolean?,
    val daLuu: Boolean?,
    val idMonAn: String?,
    val ngayTao: String?,
)

data class TrangLichSuScanDto(
    val items: List<LichSuScanOutDto>?,
    val total: Int?,
    val page: Int?,
)

data class ErrorBodyDto(
    val code: String?,
    val message: String?,
)
