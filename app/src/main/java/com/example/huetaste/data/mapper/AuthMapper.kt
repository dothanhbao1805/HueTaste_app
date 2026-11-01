package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.TokenOutDto
import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.model.UserProfile

fun TokenOutDto.toDomain() = AuthSession(
    accessToken = accessToken,
    refreshToken = refreshToken,
    user = UserProfile(
        id = nguoiDung.idNguoiDung,
        email = nguoiDung.email,
        name = nguoiDung.hoTen.orEmpty(),
        avatarUrl = nguoiDung.anhDaiDien,
    ),
)
