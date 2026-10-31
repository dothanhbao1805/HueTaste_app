package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `DiaDiemChiTietOut` schema. */
data class DiaDiemChiTietDto(
    val idDiaDiem: String,
    val tenDiaDiem: String,
    val diaChi: String?,
    val viDo: Double?,
    val kinhDo: Double?,
    val soDienThoai: String?,
    val gioMoCua: List<String>?,
    val mucGia: Int?,
    val diemTrungBinh: Double?,
    val anhUrl: String?,
    val anh: List<AnhDto>?,
    val monAn: List<MonTaiQuanDto>?,
    val daYeuThich: Boolean?,
)

data class MonTaiQuanDto(
    val monAn: MonAnDto,
    val giaBan: Int?,
    val laMonDacTrung: Boolean?,
)

/** Backend `TrangDanhGiaOut`: one page of reviews plus the place's combined average. */
data class TrangDanhGiaDto(
    val items: List<DanhGiaDto>?,
    val total: Int?,
    val page: Int?,
    val size: Int?,
    val diemTrungBinh: Double?,
)

data class DanhGiaDto(
    val idDanhGia: String,
    val diemSo: Int,
    val noiDung: String?,
    val ngayTao: String?,
    val nguoiDung: NguoiVietDto?,
)

data class NguoiVietDto(
    val hoTen: String?,
    val anhDaiDien: String?,
)

/** Body of `POST /danh-gia` for a place. */
data class VietDanhGiaBody(
    val idDiaDiem: String,
    val diemSo: Int,
    val noiDung: String?,
)

/** Body of `POST /users/me/yeu-thich` for a place. */
data class YeuThichDiaDiemBody(val idDiaDiem: String)
