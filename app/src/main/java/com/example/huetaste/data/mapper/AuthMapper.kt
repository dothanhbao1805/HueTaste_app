package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.NguoiDungDto
import com.example.huetaste.data.remote.dto.TokenOutDto
import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.model.UserProfile

fun TokenOutDto.toDomain() = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = nguoiDung.toDomain(),
)

fun NguoiDungDto.toDomain() = UserProfile(
    id = idNguoiDung,
    email = email,
    name = hoTen.orEmpty(),
    avatarUrl = anhDaiDien,
)
