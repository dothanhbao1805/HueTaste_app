package com.example.huetaste.feature.dish_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.GalleryImage
import com.example.huetaste.domain.model.DishIngredient
import com.example.huetaste.domain.model.DishPlace
import com.example.huetaste.domain.model.ScanResult
import com.example.huetaste.domain.model.ScanStatus
import com.example.huetaste.domain.usecase.GetDishDetailUseCase
import com.example.huetaste.domain.usecase.GetScanResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the dish screen shows; [scanStatus] is null when the dish was opened directly from the catalog. */
data class DishDetailContent(
    val name: String,
    val images: List<GalleryImage>,
    val categoryName: String?,
    val mealTimes: List<String>,
    val tags: List<String>,
    val priceFrom: Int?,
    val priceTo: Int?,
    val description: String?,
    val ingredients: List<DishIngredient>,
    val places: List<DishPlace>,
    val similarDishes: List<Dish>,
    val isFavorite: Boolean,
    val scanStatus: ScanStatus?,
) {
    val isCatalogDish get() = scanStatus == null || scanStatus == ScanStatus.IN_CATALOG
}

sealed interface DishDetailUiState {
    /** Opened without a dish or scan id: the screen keeps its sample content. */
    data object Sample : DishDetailUiState
    data object Loading : DishDetailUiState
    data class Content(val content: DishDetailContent) : DishDetailUiState
    data class Error(val error: AppError) : DishDetailUiState
}

@HiltViewModel
class DishDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getScanResult: GetScanResultUseCase,
    private val getDishDetail: GetDishDetailUseCase,
) : ViewModel() {

    // Intent extras arrive as the Activity's default arguments.
    private val scanId: String? = savedState[DishDetailActivity.EXTRA_SCAN_ID]
    private val dishId: String? = savedState[DishDetailActivity.EXTRA_DISH_ID]

    private val _uiState = MutableStateFlow<DishDetailUiState>(
        if (scanId == null && dishId == null) DishDetailUiState.Sample else DishDetailUiState.Loading,
    )
    val uiState: StateFlow<DishDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (_uiState.value is DishDetailUiState.Sample || loadJob?.isActive == true) return
        _uiState.value = DishDetailUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = try {
                DishDetailUiState.Content(
                    when {
                        scanId != null -> getScanResult(scanId).toContent()
                        else -> getDishDetail(requireNotNull(dishId)).let { detail ->
                            detail.dish.toContent(
                                categoryName = detail.categoryName,
                                tags = detail.tastes + detail.diets,
                                ingredients = detail.ingredients,
                                places = detail.places,
                                isFavorite = detail.isFavorite,
                                images = detail.images,
                            )
                        }
                    },
                )
            } catch (e: AppError) {
                DishDetailUiState.Error(e)
            }
        }
    }

    private fun ScanResult.toContent(): DishDetailContent {
        val detail = dish
        return if (status == ScanStatus.IN_CATALOG && detail != null) {
            detail.dish.toContent(
                categoryName = detail.categoryName,
                tags = detail.tastes + detail.diets,
                ingredients = detail.ingredients,
                places = detail.places,
                isFavorite = detail.isFavorite,
                images = detail.images,
                fallbackImage = imageUrl,
                scanStatus = status,
            )
        } else {
            // Only the recognition result is known; the photo the user took stands in for the dish image.
            DishDetailContent(
                name = recognizedName,
                images = listOf(GalleryImage(url = imageUrl, thumbnailUrl = null, source = null)),
                categoryName = null,
                mealTimes = emptyList(),
                tags = emptyList(),
                priceFrom = null,
                priceTo = null,
                description = shortDescription,
                ingredients = emptyList(),
                places = emptyList(),
                similarDishes = similarDishes,
                isFavorite = false,
                scanStatus = status,
            )
        }
    }

    private fun Dish.toContent(
        categoryName: String?,
        tags: List<String>,
        ingredients: List<DishIngredient>,
        places: List<DishPlace>,
        isFavorite: Boolean,
        images: List<GalleryImage>,
        fallbackImage: String? = null,
        scanStatus: ScanStatus? = null,
    ) = DishDetailContent(
        name = name,
        // The photo the user took stands in when the catalog has no picture of the dish.
        images = images.ifEmpty {
            listOfNotNull((imageUrl ?: fallbackImage)?.let { GalleryImage(url = it, thumbnailUrl = null, source = null) })
        },
        categoryName = categoryName,
        mealTimes = mealTimes,
        tags = tags,
        priceFrom = priceFrom,
        priceTo = priceTo,
        description = description,
        ingredients = ingredients,
        places = places,
        similarDishes = emptyList(),
        isFavorite = isFavorite,
        scanStatus = scanStatus,
    )
}
