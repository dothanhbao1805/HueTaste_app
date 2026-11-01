package com.example.huetaste.domain.model

/** Failure reported by a data source; [code] mirrors the backend error code when there is one. */
class AppError(
    val code: String,
    override val message: String?,
    cause: Throwable? = null,
) : Exception(message, cause) {
    companion object {
        const val NETWORK = "NETWORK"
        const val UNKNOWN = "UNKNOWN"
        const val NOT_RECOGNIZED = "KHONG_NHAN_DIEN_DUOC"
        const val DAILY_LIMIT = "SCAN_QUA_GIOI_HAN"
        const val FILE_TOO_LARGE = "FILE_TOO_LARGE"
        const val INVALID_IMAGE = "INVALID_IMAGE"
        const val FAVORITE_NOT_FOUND = "YEU_THICH_NOT_FOUND"
        const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
        const val EMAIL_EXISTS = "EMAIL_EXISTS"
        const val ACCOUNT_BLOCKED = "ACCOUNT_BLOCKED"
        const val TOKEN_EXPIRED = "TOKEN_EXPIRED"
        const val INVALID_TOKEN = "INVALID_TOKEN"
        const val UNAUTHORIZED = "UNAUTHORIZED"
    }
}
