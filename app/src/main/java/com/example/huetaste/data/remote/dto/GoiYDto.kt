package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `/goi-y` schemas. Null fields are left out of the request, so saved needs stay. */
data class GoiYBody(
    val buaAn: String?,
    val nganSach: Int?,
    val ghiChu: String?,
    val soMon: Int,
    val soDiaDiem: Int,
    val viDo: Double? = null,
    val kinhDo: Double? = null,
)

data class NguCanhDto(
    val thoiTiet: String?,
    val nhietDo: Double?,
)

data class MonGoiYDto(
    val monAn: MonAnDto,
    val diemSo: Double?,
    val lyDoGiaiThich: String?,
)

data class DiaDiemGoiYDto(
    val diaDiem: DiaDiemDto,
    val diemSo: Double?,
    val lyDoGiaiThich: String?,
    val monLienQuan: List<MonAnDto>?,
)

data class KetQuaGoiYDto(
    val nguCanh: NguCanhDto?,
    val monAn: List<MonGoiYDto>?,
    val diaDiem: List<DiaDiemGoiYDto>? = null,
)
