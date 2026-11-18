package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.CheDoAnOutDto
import com.example.huetaste.data.remote.dto.NguyenLieuOutDto
import com.example.huetaste.data.remote.dto.NhuCauBody
import com.example.huetaste.data.remote.dto.NhuCauOutDto
import com.example.huetaste.data.remote.dto.SoThichOutDto
import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.domain.model.DietOption
import com.example.huetaste.domain.model.Ingredient
import com.example.huetaste.domain.model.MealBudget
import com.example.huetaste.domain.model.MealType
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
    ghiChu = note?.takeIf { it.isNotBlank() },
)

/** Meals this app does not show (the backend also knows "chieu") are left out. */
fun NhuCauOutDto.toDomainOrNull(): DailyNeed? {
    val meal = MealType.entries.firstOrNull { it.apiValue == buaAn } ?: return null
    return DailyNeed(
        meal = meal,
        time = gioAn?.take(5) ?: meal.defaultTime,
        // "Trên 100k" is saved without a cap, so it reads back as no budget.
        budget = nganSach?.let(::budgetOf),
        avoidIngredientIds = nguyenLieuTranh.orEmpty().map { it.idNguyenLieu },
        note = ghiChu,
        id = idNhuCauTheoNgay,
    )
}

private fun budgetOf(vnd: Long): MealBudget =
    MealBudget.entries.firstOrNull { cap -> cap.maxVnd != null && vnd <= cap.maxVnd } ?: MealBudget.OVER_100K
