package com.example.huetaste.data.remote

import com.example.huetaste.data.remote.dto.DanhGiaDto
import com.example.huetaste.data.remote.dto.DiaDiemChiTietDto
import com.example.huetaste.data.remote.dto.MonAnChiTietDto
import com.example.huetaste.data.remote.dto.ScanOutDto
import com.example.huetaste.data.remote.dto.TrangDanhGiaDto
import com.example.huetaste.data.remote.dto.VietDanhGiaBody
import com.example.huetaste.data.remote.dto.YeuThichDiaDiemBody
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
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
}
