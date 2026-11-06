package com.example.huetaste.data.remote

import com.example.huetaste.data.remote.dto.CapNhatCheDoAnBody
import com.example.huetaste.data.remote.dto.CapNhatSoThichBody
import com.example.huetaste.data.remote.dto.CheDoAnOutDto
import com.example.huetaste.data.remote.dto.CuocTroChuyenDto
import com.example.huetaste.data.remote.dto.GuiTinNhanBody
import com.example.huetaste.data.remote.dto.TraLoiDto
import com.example.huetaste.data.remote.dto.TrangCuocTroChuyenDto
import com.example.huetaste.data.remote.dto.TrangTinNhanDto
import com.example.huetaste.data.remote.dto.DanhGiaDto
import com.example.huetaste.data.remote.dto.DiaDiemChiTietDto
import com.example.huetaste.data.remote.dto.MonAnChiTietDto
import com.example.huetaste.data.remote.dto.NguyenLieuOutDto
import com.example.huetaste.data.remote.dto.NhuCauBody
import com.example.huetaste.data.remote.dto.ScanOutDto
import com.example.huetaste.data.remote.dto.SoThichOutDto
import com.example.huetaste.data.remote.dto.TrangDanhGiaDto
import com.example.huetaste.data.remote.dto.VietDanhGiaBody
import com.example.huetaste.data.remote.dto.YeuThichDiaDiemBody
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface HueTasteApi {
    @Multipart
    @POST("scan")
    suspend fun scan(@Part file: MultipartBody.Part): ScanOutDto

    @GET("scan/{idLichSuScan}")
    suspend fun getScan(@Path("idLichSuScan") scanId: String): ScanOutDto

    @GET("mon-an/{idMonAn}")
    suspend fun getDishDetail(@Path("idMonAn") dishId: String): MonAnChiTietDto

    @GET("dia-diem/{idDiaDiem}")
    suspend fun getPlaceDetail(@Path("idDiaDiem") placeId: String): DiaDiemChiTietDto

    @GET("dia-diem/{idDiaDiem}/danh-gia")
    suspend fun getPlaceReviews(
        @Path("idDiaDiem") placeId: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): TrangDanhGiaDto

    @POST("danh-gia")
    suspend fun submitReview(@Body body: VietDanhGiaBody): DanhGiaDto

    @POST("users/me/yeu-thich")
    suspend fun addFavoritePlace(@Body body: YeuThichDiaDiemBody)

    @DELETE("users/me/yeu-thich/dia-diem/{idDiaDiem}")
    suspend fun removeFavoritePlace(@Path("idDiaDiem") placeId: String)

    @GET("che-do-an")
    suspend fun getDietOptions(): List<CheDoAnOutDto>

    @GET("so-thich")
    suspend fun getTasteTags(): List<SoThichOutDto>

    @GET("nguyen-lieu")
    suspend fun searchIngredients(@Query("q") query: String?, @Query("size") size: Int): List<NguyenLieuOutDto>

    @GET("users/me/che-do-an")
    suspend fun getMyDiets(): List<CheDoAnOutDto>

    @PUT("users/me/che-do-an")
    suspend fun saveMyDiets(@Body body: CapNhatCheDoAnBody)

    @GET("users/me/so-thich")
    suspend fun getMyTastes(): List<SoThichOutDto>

    @PUT("users/me/so-thich")
    suspend fun saveMyTastes(@Body body: CapNhatSoThichBody)

    @PUT("users/me/nhu-cau")
    suspend fun saveDailyNeed(@Body body: NhuCauBody)

    @POST("tro-chuyen")
    suspend fun createConversation(): CuocTroChuyenDto

    @GET("tro-chuyen")
    suspend fun getConversations(@Query("page") page: Int, @Query("size") size: Int): TrangCuocTroChuyenDto

    @GET("tro-chuyen/{idCuoc}/tin-nhan")
    suspend fun getMessages(
        @Path("idCuoc") conversationId: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): TrangTinNhanDto

    /** The assistant answers in about 3-8 seconds; the client read timeout covers it. */
    @POST("tro-chuyen/{idCuoc}/tin-nhan")
    suspend fun sendMessage(@Path("idCuoc") conversationId: String, @Body body: GuiTinNhanBody): TraLoiDto

    @DELETE("tro-chuyen/{idCuoc}")
    suspend fun deleteConversation(@Path("idCuoc") conversationId: String)
}
