package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.DiaDiemGoiYDto
import com.example.huetaste.data.remote.dto.KetQuaGoiYDto
import com.example.huetaste.data.remote.dto.MonGoiYDto
import com.example.huetaste.data.remote.dto.NguCanhDto
import com.example.huetaste.domain.model.DishSuggestion
import com.example.huetaste.domain.model.PlaceSuggestion
import com.example.huetaste.domain.model.SuggestionContext
import com.example.huetaste.domain.model.SuggestionResult

fun NguCanhDto.toDomain() = SuggestionContext(weather = thoiTiet, temperature = nhietDo)

fun MonGoiYDto.toDomain() = DishSuggestion(
    dish = monAn.toDomain(),
    score = diemSo,
    reason = lyDoGiaiThich?.takeIf { it.isNotBlank() },
)

fun DiaDiemGoiYDto.toDomain() = PlaceSuggestion(
    place = diaDiem.toDomain(),
    distanceKm = diaDiem.khoangCachKm,
    score = diemSo,
    reason = lyDoGiaiThich?.takeIf { it.isNotBlank() },
    relatedDishes = monLienQuan.orEmpty().map { it.toDomain() },
)

fun KetQuaGoiYDto.toDomain() = SuggestionResult(
    context = nguCanh?.toDomain(),
    dishes = monAn.orEmpty().map { it.toDomain() },
    places = diaDiem.orEmpty().map { it.toDomain() },
)
