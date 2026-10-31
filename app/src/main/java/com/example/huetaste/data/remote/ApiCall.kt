package com.example.huetaste.data.remote

import com.example.huetaste.data.remote.dto.ErrorBodyDto
import com.example.huetaste.domain.model.AppError
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

/** Runs [block] and reports every failure as an [AppError] carrying the backend `{code, message}` when present. */
internal suspend fun <T> apiCall(gson: Gson, block: suspend () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val body = runCatching {
        e.response()?.errorBody()?.charStream()?.use { gson.fromJson(it, ErrorBodyDto::class.java) }
    }.getOrNull()
    throw AppError(body?.code ?: "HTTP_${e.code()}", body?.message, e)
} catch (e: IOException) {
    throw AppError(AppError.NETWORK, e.message, e)
} catch (e: RuntimeException) {
    // Gson and the DTO mappers report malformed bodies as runtime exceptions.
    throw AppError(AppError.UNKNOWN, e.message, e)
}
