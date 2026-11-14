package com.example.huetaste.data.remote.dto

/** Body of `POST /bao-cao`; field names mirror the backend `GuiBaoCao` schema. */
data class GuiBaoCaoBody(
    val idDiaDiem: String,
    val lyDo: String,
    val moTa: String?,
    val idMonAn: List<String>,
)

/** Backend `BaoCaoOut` as the reporting user sees it (the admin-only `nguoiBaoCao` is left out). */
data class BaoCaoDto(
    val idBaoCao: String,
    val diaDiem: DiaDiemBaoCaoDto?,
    val lyDo: String?,
    val moTa: String?,
    val trangThai: String?,
    val ketQuaXuLy: String?,
    val xuLyLuc: String?,
    val ngayTao: String?,
    val chiTiet: List<ChiTietBaoCaoDto>?,
)

data class DiaDiemBaoCaoDto(
    val idDiaDiem: String,
    val tenDiaDiem: String?,
)

data class ChiTietBaoCaoDto(
    val idChiTietBaoCao: String,
    val monAn: MonBaoCaoDto?,
    val ketQuaXuLy: String?,
)

data class MonBaoCaoDto(
    val idMonAn: String,
    val tenMon: String?,
)

/** Backend `Page[BaoCaoOut]` of `GET /users/me/bao-cao`. */
data class TrangBaoCaoDto(
    val items: List<BaoCaoDto>?,
    val total: Int?,
    val page: Int?,
    val size: Int?,
)
