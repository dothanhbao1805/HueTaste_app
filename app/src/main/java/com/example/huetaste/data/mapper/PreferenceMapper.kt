package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.CheDoAnOutDto
import com.example.huetaste.data.remote.dto.NguyenLieuOutDto
import com.example.huetaste.data.remote.dto.NhuCauBody
import com.example.huetaste.data.remote.dto.SoThichOutDto
import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.model.DietOption
import com.example.huetaste.domain.model.Ingredient
import com.example.huetaste.domain.model.TasteTag
import com.example.huetaste.domain.model.TasteTagType

private const val TYPE_FLAVOR = "vi"
private const val TYPE_STYLE = "kieu"

fun CheDoAnOutDto.toDomain() = DietOption(id = idCheDoAn, name = tenCheDo, description = moTa?.takeIf { it.isNotBlank() })

/** Tags of a type this app does not know are left out. */
fun SoThichOutDto.toDomainOrNull(): TasteTag? {
    val type = when (loai) {
        TYPE_FLAVOR -> TasteTagType.FLAVOR
        TYPE_STYLE -> TasteTagType.STYLE
        else -> return null
    }
    return TasteTag(id = idSoThich, type = type, name = tenSoThich)
}

fun NguyenLieuOutDto.toDomain() = Ingredient(id = idNguyenLieu, name = tenNguyenLieu, group = nhomNguyenLieu)

fun DailyNeed.toBody() = NhuCauBody(
    buaAn = meal.apiValue,
    gioAn = time,
    nganSach = budget?.maxVnd,
    nguyenLieuTranh = avoidIngredientIds,
)
