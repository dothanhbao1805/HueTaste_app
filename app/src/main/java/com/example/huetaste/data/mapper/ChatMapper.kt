package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.CuocTroChuyenDto
import com.example.huetaste.data.remote.dto.TheDiaDiemDto
import com.example.huetaste.data.remote.dto.TheMonDto
import com.example.huetaste.data.remote.dto.TinNhanDto
import com.example.huetaste.data.remote.dto.TraLoiDto
import com.example.huetaste.data.remote.dto.TrangCuocTroChuyenDto
import com.example.huetaste.domain.model.ChatConversation
import com.example.huetaste.domain.model.ChatDishSuggestion
import com.example.huetaste.domain.model.ChatMessage
import com.example.huetaste.domain.model.ChatMessageKind
import com.example.huetaste.domain.model.ChatPlaceSuggestion
import com.example.huetaste.domain.model.ChatReply
import com.example.huetaste.domain.model.ChatSender
import com.example.huetaste.domain.model.ConversationPage
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

private const val SENDER_USER = "NGUOI_DUNG"
private const val KIND_SUGGESTION = "goi_y"

fun CuocTroChuyenDto.toDomain() = ChatConversation(
    id = idCuocTroChuyen,
    title = tieuDe?.takeIf { it.isNotBlank() },
    createdAt = parseBackendTime(ngayTao),
)

fun TrangCuocTroChuyenDto.toDomain(requestedPage: Int, requestedSize: Int) = ConversationPage(
    items = items.orEmpty().map { it.toDomain() },
    total = total ?: 0,
    page = page ?: requestedPage,
    size = size ?: requestedSize,
)

fun TinNhanDto.toDomain() = ChatMessage(
    id = idTinNhan,
    sender = if (nguoiGui == SENDER_USER) ChatSender.USER else ChatSender.ASSISTANT,
    kind = if (loaiNoiDung == KIND_SUGGESTION) ChatMessageKind.SUGGESTION else ChatMessageKind.TEXT,
    text = noiDung.orEmpty(),
    createdAt = parseBackendTime(ngayTao),
    score = diemSo,
    dish = monAn?.toDomain(),
    place = diaDiem?.toDomain(),
)

fun TheMonDto.toDomain() = ChatDishSuggestion(
    dish = monAn.toDomain(),
    score = diemSo,
    reason = lyDoGiaiThich?.takeIf { it.isNotBlank() },
)

fun TheDiaDiemDto.toDomain() = ChatPlaceSuggestion(
    place = diaDiem.toDomain(),
    distanceKm = diaDiem.khoangCachKm,
    score = diemSo,
    reason = lyDoGiaiThich?.takeIf { it.isNotBlank() },
)

fun TraLoiDto.toDomain() = ChatReply(
    userMessage = tinNhanNguoiDung.toDomain(),
    reply = traLoi.toDomain(),
    dishes = monAn.orEmpty().map { it.toDomain() },
    places = diaDiem.orEmpty().map { it.toDomain() },
)

/**
 * The backend writes `ngayTao` as a local date-time without a zone (`2026-11-05T11:42:31`, sometimes with
 * fractional seconds), in the time zone of the server, which is the user's own in Huế.
 * Unreadable values become 0, which sorts first.
 */
internal fun parseBackendTime(value: String?): Long {
    if (value.isNullOrBlank()) return 0L
    val withoutFraction = value.substringBefore('.').substringBefore('Z').take(19)
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(withoutFraction)?.time ?: 0L
    } catch (e: ParseException) {
        0L
    }
}
