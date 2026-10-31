package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `MonAnOut` / `MonAnChiTietOut` schemas. */
data class MonAnDto(
    val idMonAn: String,
    val tenMon: String,
    val moTa: String?,
    val hinhAnh: String?,
    val buoiPhuHop: List<String>?,
    val giaThamKhaoTu: Int?,
    val giaThamKhaoDen: Int?,
)

data class MonAnChiTietDto(
    val idMonAn: String,
    val tenMon: String,
    val moTa: String?,
    val hinhAnh: String?,
    val buoiPhuHop: List<String>?,
    val giaThamKhaoTu: Int?,
    val giaThamKhaoDen: Int?,
    val danhMuc: DanhMucDto?,
    val nguyenLieu: List<NguyenLieuDto>?,
    val soThich: List<SoThichDto>?,
    val cheDoAn: List<CheDoAnDto>?,
    val diaDiem: List<MonAnDiaDiemDto>?,
    val daYeuThich: Boolean?,
    val anh: List<AnhDto>?,
)

data class AnhDto(
    val url: String,
    val urlThumbnail: String?,
    val nguon: String?,
)

data class DanhMucDto(val tenDanhMuc: String?)

data class NguyenLieuDto(
    val tenNguyenLieu: String,
    val nhomNguyenLieu: String?,
    val laThanhPhanChinh: Boolean?,
)

data class SoThichDto(val tenSoThich: String)

data class CheDoAnDto(val tenCheDo: String)

data class MonAnDiaDiemDto(
    val diaDiem: DiaDiemDto,
    val giaBan: Int?,
    val laMonDacTrung: Boolean?,
)

data class DiaDiemDto(
    val idDiaDiem: String,
    val tenDiaDiem: String,
    val diaChi: String?,
    val viDo: Double?,
    val kinhDo: Double?,
    val gioMoCua: List<String>?,
    val mucGia: Int?,
    val diemTrungBinh: Double?,
    val anhUrl: String?,
)
