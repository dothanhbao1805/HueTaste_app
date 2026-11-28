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

/** Mirror the backend `QuenMatKhau`, `XacThucOtp` and `DatLaiMatKhau` schemas. */
data class QuenMatKhauBody(val email: String)

data class XacThucOtpBody(
    val email: String,
    val otp: String,
)

/** Mirrors the backend `CapNhatThongTin` schema; the avatar has its own upload call. */
data class CapNhatThongTinBody(val hoTen: String)

/** Mirrors the backend `DoiMatKhau` schema. */
data class DoiMatKhauBody(
    val matKhauCu: String,
    val matKhauMoi: String,
)

data class DatLaiMatKhauBody(
    val email: String,
    val otp: String,
    val matKhauMoi: String,
)

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
