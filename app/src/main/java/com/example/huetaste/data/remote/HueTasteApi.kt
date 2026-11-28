package com.example.huetaste.data.remote

import com.example.huetaste.data.remote.dto.BaoCaoDto
import com.example.huetaste.data.remote.dto.CapNhatThongTinBody
import com.example.huetaste.data.remote.dto.CapNhatCheDoAnBody
import com.example.huetaste.data.remote.dto.GuiBaoCaoBody
import com.example.huetaste.data.remote.dto.TrangBaoCaoDto
import com.example.huetaste.data.remote.dto.CapNhatSoThichBody
import com.example.huetaste.data.remote.dto.CheDoAnOutDto
import com.example.huetaste.data.remote.dto.CuocTroChuyenDto
import com.example.huetaste.data.remote.dto.GuiTinNhanBody
import com.example.huetaste.data.remote.dto.TraLoiDto
import com.example.huetaste.data.remote.dto.TrangCuocTroChuyenDto
import com.example.huetaste.data.remote.dto.TrangTinNhanDto
import com.example.huetaste.data.remote.dto.DanhGiaDto
import com.example.huetaste.data.remote.dto.DanhMucOutDto
import com.example.huetaste.data.remote.dto.GoiYBody
import com.example.huetaste.data.remote.dto.KetQuaGoiYDto
import com.example.huetaste.data.remote.dto.TrangDiaDiemDto
import com.example.huetaste.data.remote.dto.TrangThongBaoDto
import com.example.huetaste.data.remote.dto.TrangMonAnDto
import com.example.huetaste.data.remote.dto.YeuThichDto
import com.example.huetaste.data.remote.dto.YeuThichMonAnBody
import com.example.huetaste.data.remote.dto.DiaDiemChiTietDto
import com.example.huetaste.data.remote.dto.DoiMatKhauBody
import com.example.huetaste.data.remote.dto.MonAnChiTietDto
import com.example.huetaste.data.remote.dto.NguoiDungDto
import com.example.huetaste.data.remote.dto.NguyenLieuOutDto
import com.example.huetaste.data.remote.dto.NhuCauBody
import com.example.huetaste.data.remote.dto.NhuCauOutDto
import com.example.huetaste.data.remote.dto.TrangLichSuScanDto
import com.example.huetaste.data.remote.dto.ScanOutDto
import com.example.huetaste.data.remote.dto.SoChuaDocDto
import com.example.huetaste.data.remote.dto.SoThichOutDto
import com.example.huetaste.data.remote.dto.TrangDanhGiaDto
import com.example.huetaste.data.remote.dto.VietDanhGiaBody
import com.example.huetaste.data.remote.dto.YeuThichDiaDiemBody
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface HueTasteApi {
    @GET("users/me/thong-bao")
    suspend fun getNotifications(@Query("page") page: Int, @Query("size") size: Int): TrangThongBaoDto

    @GET("users/me/thong-bao/chua-doc")
    suspend fun getUnreadNotificationCount(): SoChuaDocDto

    @POST("users/me/thong-bao/{idThongBao}/da-doc")
    suspend fun markNotificationRead(@Path("idThongBao") notificationId: String)

    @POST("users/me/thong-bao/da-doc-tat-ca")
    suspend fun markAllNotificationsRead()

    @PATCH("users/me")
    suspend fun updateProfile(@Body body: CapNhatThongTinBody): NguoiDungDto

    @Multipart
    @POST("users/me/avatar")
    suspend fun uploadAvatar(@Part file: MultipartBody.Part): NguoiDungDto

    @POST("users/me/change-password")
    suspend fun changePassword(@Body body: DoiMatKhauBody)

    @Multipart
    @POST("scan")
    suspend fun scan(@Part file: MultipartBody.Part): ScanOutDto

    @GET("scan/{idLichSuScan}")
    suspend fun getScan(@Path("idLichSuScan") scanId: String): ScanOutDto

    @GET("mon-an/{idMonAn}")
    suspend fun getDishDetail(@Path("idMonAn") dishId: String): MonAnChiTietDto

    @GET("dia-diem")
    suspend fun getNearbyPlaces(
        @Query("viDo") latitude: Double,
        @Query("kinhDo") longitude: Double,
        @Query("banKinhKm") radiusKm: Double,
        @Query("size") size: Int,
    ): TrangDiaDiemDto

    /** `khuVuc` is `co_do`, `bo_bac` or `bo_nam`; the location pair is optional but must be sent together. */
    @GET("dia-diem")
    suspend fun searchPlaces(
        @Query("q") query: String?,
        @Query("viDo") latitude: Double?,
        @Query("kinhDo") longitude: Double?,
        @Query("banKinhKm") radiusKm: Double?,
        @Query("khuVuc") area: String?,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): TrangDiaDiemDto

    @GET("dia-diem/noi-bat")
    suspend fun getFeaturedPlaces(@Query("size") size: Int): TrangDiaDiemDto

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

    @POST("bao-cao")
    suspend fun submitReport(@Body body: GuiBaoCaoBody): BaoCaoDto

    @GET("users/me/bao-cao")
    suspend fun getMyReports(@Query("page") page: Int, @Query("size") size: Int): TrangBaoCaoDto

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

    /** Without `ngay` the backend returns today's needs (Vietnam time). */
    @GET("users/me/nhu-cau")
    suspend fun getMyNeeds(): List<NhuCauOutDto>

    @DELETE("users/me/nhu-cau/{idNhuCau}")
    suspend fun deleteDailyNeed(@Path("idNhuCau") needId: String)

    @GET("users/me/lich-su-scan")
    suspend fun getScanHistory(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("loc") filter: String,
    ): TrangLichSuScanDto

    @DELETE("users/me/lich-su-scan/{idLichSuScan}")
    suspend fun deleteScan(@Path("idLichSuScan") scanId: String)

    @POST("users/me/lich-su-scan/{idLichSuScan}/khoi-phuc")
    suspend fun restoreScan(@Path("idLichSuScan") scanId: String)

    @POST("goi-y")
    suspend fun getSuggestions(@Body body: GoiYBody): KetQuaGoiYDto

    @GET("danh-muc")
    suspend fun getCategories(): List<DanhMucOutDto>

    @GET("mon-an")
    suspend fun getDishes(@Query("page") page: Int, @Query("size") size: Int): TrangMonAnDto

    /** Each `nguyenLieu` is sent as its own query pair; the backend returns dishes that have all of them. */
    @GET("mon-an")
    suspend fun searchDishes(
        @Query("q") query: String?,
        @Query("idDanhMucMonAn") categoryId: String?,
        @Query("nguyenLieu") ingredients: List<String>,
        @Query("sapXep") sort: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): TrangMonAnDto

    @GET("users/me/yeu-thich")
    suspend fun getFavorites(@Query("loai") type: String? = null): YeuThichDto

    @POST("users/me/yeu-thich")
    suspend fun addFavoriteDish(@Body body: YeuThichMonAnBody)

    @DELETE("users/me/yeu-thich/mon-an/{idMonAn}")
    suspend fun removeFavoriteDish(@Path("idMonAn") dishId: String)

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
