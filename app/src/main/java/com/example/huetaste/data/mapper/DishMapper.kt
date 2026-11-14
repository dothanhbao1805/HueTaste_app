package com.example.huetaste.data.mapper

import com.example.huetaste.data.remote.dto.DanhMucOutDto
import com.example.huetaste.data.remote.dto.DiaDiemDto
import com.example.huetaste.data.remote.dto.MonAnChiTietDto
import com.example.huetaste.data.remote.dto.MonAnDto
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.DishCategory
import com.example.huetaste.domain.model.DishDetail
import com.example.huetaste.domain.model.GalleryImage
import com.example.huetaste.domain.model.HomePlace
import com.example.huetaste.domain.model.DishIngredient
import com.example.huetaste.domain.model.DishPlace
import com.example.huetaste.domain.model.Place

fun MonAnDto.toDomain() = Dish(
    id = idMonAn,
    name = tenMon,
    description = moTa,
    imageUrl = hinhAnh,
    mealTimes = buoiPhuHop.orEmpty(),
    priceFrom = giaThamKhaoTu,
    priceTo = giaThamKhaoDen,
    categoryId = idDanhMucMonAn,
    weatherTags = thoiTietPhuHop.orEmpty(),
)

fun DanhMucOutDto.toDomain() = DishCategory(id = idDanhMucMonAn, name = tenDanhMuc)

fun MonAnChiTietDto.toDomain() = DishDetail(
    dish = Dish(
        id = idMonAn,
        name = tenMon,
        description = moTa,
        imageUrl = hinhAnh,
        mealTimes = buoiPhuHop.orEmpty(),
        priceFrom = giaThamKhaoTu,
        priceTo = giaThamKhaoDen,
        weatherTags = thoiTietPhuHop.orEmpty(),
    ),
    categoryName = danhMuc?.tenDanhMuc,
    ingredients = nguyenLieu.orEmpty().map {
        DishIngredient(name = it.tenNguyenLieu, group = it.nhomNguyenLieu, isMain = it.laThanhPhanChinh == true)
    },
    tastes = soThich.orEmpty().map { it.tenSoThich },
    diets = cheDoAn.orEmpty().map { it.tenCheDo },
    places = diaDiem.orEmpty().map {
        DishPlace(place = it.diaDiem.toDomain(), price = it.giaBan, isSignature = it.laMonDacTrung == true)
    },
    isFavorite = daYeuThich == true,
    images = anh.orEmpty().map { GalleryImage(url = it.url, thumbnailUrl = it.urlThumbnail, source = it.nguon) }
        // Older rows only have the cover image; keep showing it when the gallery is empty.
        .ifEmpty { listOfNotNull(hinhAnh?.let { GalleryImage(url = it, thumbnailUrl = null, source = null) }) },
)

fun DiaDiemDto.toDomain() = Place(
    id = idDiaDiem,
    name = tenDiaDiem,
    address = diaChi,
    latitude = viDo,
    longitude = kinhDo,
    openingHours = gioMoCua.orEmpty(),
    priceLevel = mucGia,
    rating = diemTrungBinh,
    imageUrl = anhUrl,
    phone = soDienThoai?.takeIf { it.isNotBlank() },
)

fun DiaDiemDto.toHomePlace() = HomePlace(place = toDomain(), distanceKm = khoangCachKm)
