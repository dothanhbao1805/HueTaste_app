package com.example.huetaste.data.remote.dto

/** Field names mirror the backend `DangNhap`, `DangKy`, `RefreshRequest` and `TokenOut` schemas. */
data class DangNhapBody(
    val email: String,
    val matKhau: String,
)

data class DangKyBody(
    val email: String,
    val matKhau: String,
    val hoTen: String,
)

data class RefreshBody(val refreshToken: String)

data class NguoiDungDto(
    val idNguoiDung: String,
    val email: String,
    val hoTen: String?,
    val anhDaiDien: String?,
)

data class TokenOutDto(
    val accessToken: String,
    val refreshToken: String,
    val nguoiDung: NguoiDungDto,
)
