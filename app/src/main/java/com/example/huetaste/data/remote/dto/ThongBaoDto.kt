package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `ThongBaoOut`, `Page[ThongBaoOut]` and `SoChuaDocOut` schemas. */
data class ThongBaoDto(
    val idThongBao: String,
    val loai: String?,
    val tieuDe: String?,
    val noiDung: String?,
    val idMonAn: String?,
    val idDiaDiem: String?,
    val idBaoCao: String?,
    val daDoc: Boolean?,
    val ngayTao: String?,
)

data class TrangThongBaoDto(
    val items: List<ThongBaoDto>?,
    val total: Int?,
    val page: Int?,
    val size: Int?,
)

data class SoChuaDocDto(val soChuaDoc: Int?)
