package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `/tro-chuyen` schemas (`app/schemas/tro_chuyen.py`). */
data class CuocTroChuyenDto(
    val idCuocTroChuyen: String,
    val tieuDe: String?,
    val ngayTao: String?,
)

data class TrangCuocTroChuyenDto(
    val items: List<CuocTroChuyenDto>?,
    val total: Int?,
    val page: Int?,
    val size: Int?,
)

/** `nguoiGui` is `NGUOI_DUNG` or `TRO_LY`; `loaiNoiDung` is `text` or `goi_y` (with `monAn` or `diaDiem`). */
data class TinNhanDto(
    val idTinNhan: String,
    val nguoiGui: String?,
    val loaiNoiDung: String?,
    val noiDung: String?,
    val ngayTao: String?,
    val diemSo: Double?,
    val monAn: MonAnDto?,
    val diaDiem: DiaDiemDto?,
)

data class TrangTinNhanDto(
    val items: List<TinNhanDto>?,
    val total: Int?,
    val page: Int?,
    val size: Int?,
)

data class GuiTinNhanBody(
    val noiDung: String,
    val viDo: Double?,
    val kinhDo: Double?,
)

data class TheMonDto(
    val monAn: MonAnDto,
    val diemSo: Double?,
    val lyDoGiaiThich: String?,
)

data class TheDiaDiemDto(
    val diaDiem: DiaDiemDto,
    val diemSo: Double?,
    val lyDoGiaiThich: String?,
)

data class TraLoiDto(
    val tinNhanNguoiDung: TinNhanDto,
    val traLoi: TinNhanDto,
    val monAn: List<TheMonDto>?,
    val diaDiem: List<TheDiaDiemDto>?,
)
