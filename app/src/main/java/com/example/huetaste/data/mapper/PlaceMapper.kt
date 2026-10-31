package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.DanhGiaDto
import com.example.huetaste.data.remote.dto.DiaDiemChiTietDto
import com.example.huetaste.data.remote.dto.TrangDanhGiaDto
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.GalleryImage
import com.example.huetaste.domain.model.Place
import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.model.PlaceMenuItem
import com.example.huetaste.domain.model.Review
import com.example.huetaste.domain.model.ReviewPage

fun DiaDiemChiTietDto.toDomain(): PlaceDetail {
    val gallery = anh.orEmpty().map { GalleryImage(url = it.url, thumbnailUrl = it.urlThumbnail, source = it.nguon) }
    // The backend gallery leaves out the cover photo; show it first so the hero matches the list cards.
    val images = if (anhUrl != null && gallery.none { it.url == anhUrl }) {
        listOf(GalleryImage(url = anhUrl, thumbnailUrl = null, source = null)) + gallery
    } else {
        gallery
    }
    return PlaceDetail(
        place = Place(
            id = idDiaDiem,
            name = tenDiaDiem,
            address = diaChi,
            latitude = viDo,
            longitude = kinhDo,
            openingHours = gioMoCua.orEmpty(),
            priceLevel = mucGia,
            rating = diemTrungBinh,
            imageUrl = anhUrl,
        ),
        phone = soDienThoai?.takeIf { it.isNotBlank() },
        images = images,
        menu = monAn.orEmpty().map {
            PlaceMenuItem(
                dish = Dish(
                    id = it.monAn.idMonAn,
                    name = it.monAn.tenMon,
                    description = it.monAn.moTa,
                    imageUrl = it.monAn.hinhAnh,
                    mealTimes = it.monAn.buoiPhuHop.orEmpty(),
                    priceFrom = it.monAn.giaThamKhaoTu,
                    priceTo = it.monAn.giaThamKhaoDen,
                ),
                price = it.giaBan,
                isSignature = it.laMonDacTrung == true,
            )
        },
        isFavorite = daYeuThich == true,
    )
}

fun DanhGiaDto.toDomain() = Review(
    id = idDanhGia,
    rating = diemSo,
    content = noiDung?.takeIf { it.isNotBlank() },
    createdAt = ngayTao,
    authorName = nguoiDung?.hoTen,
    authorAvatarUrl = nguoiDung?.anhDaiDien,
)

fun TrangDanhGiaDto.toDomain() = ReviewPage(
    reviews = items.orEmpty().map { it.toDomain() },
    total = total ?: 0,
    page = page ?: 1,
    averageRating = diemTrungBinh,
)
