package com.example.huetaste.feature.place_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.model.Review
import com.example.huetaste.domain.model.ReviewPage
import com.example.huetaste.domain.usecase.GetPlaceDetailUseCase
import com.example.huetaste.domain.usecase.GetPlaceReviewsUseCase
import com.example.huetaste.domain.usecase.SetPlaceFavoriteUseCase
import com.example.huetaste.domain.usecase.SubmitPlaceReviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the place screen shows once the place has loaded. */
data class PlaceDetailContent(
    val detail: PlaceDetail,
    val isFavorite: Boolean,
    val reviews: List<Review>,
    /** Reviews written in the app; the combined average also counts Google. */
    val reviewTotal: Int,
    val averageRating: Double?,
    val hasMoreReviews: Boolean,
    val isLoadingMoreReviews: Boolean,
    val nextReviewPage: Int,
)

sealed interface PlaceDetailUiState {
    data object Loading : PlaceDetailUiState
    data class Content(val content: PlaceDetailContent) : PlaceDetailUiState
    data class Error(val error: AppError) : PlaceDetailUiState
}

/** Progress of the review being written in the review sheet. */
sealed interface ReviewSubmitState {
    data object Idle : ReviewSubmitState
    data object Submitting : ReviewSubmitState
    data class Failed(val error: AppError) : ReviewSubmitState
    data object Submitted : ReviewSubmitState
}

/** One-off messages for the screen. */
sealed interface PlaceDetailEvent {
    data object FavoriteFailed : PlaceDetailEvent
    data object ReviewSubmitted : PlaceDetailEvent
}

@HiltViewModel
class PlaceDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val getPlaceDetail: GetPlaceDetailUseCase,
    private val getPlaceReviews: GetPlaceReviewsUseCase,
    private val submitPlaceReview: SubmitPlaceReviewUseCase,
    private val setPlaceFavorite: SetPlaceFavoriteUseCase,
) : ViewModel() {

    // Intent extras arrive as the Activity's default arguments.
    private val placeId: String = requireNotNull(savedState[PlaceDetailActivity.EXTRA_PLACE_ID]) {
        "PlaceDetailActivity needs EXTRA_PLACE_ID"
    }

    private val _uiState = MutableStateFlow<PlaceDetailUiState>(PlaceDetailUiState.Loading)
    val uiState: StateFlow<PlaceDetailUiState> = _uiState.asStateFlow()

    private val _reviewState = MutableStateFlow<ReviewSubmitState>(ReviewSubmitState.Idle)
    val reviewState: StateFlow<ReviewSubmitState> = _reviewState.asStateFlow()

    private val _events = Channel<PlaceDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loadJob: Job? = null
    private var moreReviewsJob: Job? = null
    private var favoriteJob: Job? = null
    private var submitJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        _uiState.value = PlaceDetailUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = try {
                coroutineScope {
                    val detail = async { getPlaceDetail(placeId) }
                    // The reviews are secondary: the place still shows when they cannot be loaded.
                    val reviews = async { runCatching { getPlaceReviews(placeId, FIRST_PAGE) }.getOrNull() }
                    PlaceDetailUiState.Content(detail.await().toContent(reviews.await()))
                }
            } catch (e: AppError) {
                PlaceDetailUiState.Error(e)
            }
        }
    }

    fun loadMoreReviews() {
        val content = currentContent() ?: return
        if (!content.hasMoreReviews || content.isLoadingMoreReviews || moreReviewsJob?.isActive == true) return
        updateContent { it.copy(isLoadingMoreReviews = true) }
        moreReviewsJob = viewModelScope.launch {
            try {
                val page = getPlaceReviews(placeId, content.nextReviewPage)
                updateContent {
                    it.copy(
                        reviews = it.reviews + page.reviews.filter { new -> it.reviews.none { old -> old.id == new.id } },
                        hasMoreReviews = page.hasMore,
                        isLoadingMoreReviews = false,
                        nextReviewPage = page.page + 1,
                    )
                }
            } catch (e: AppError) {
                // The button stays available so the user can try again.
                updateContent { it.copy(isLoadingMoreReviews = false) }
            }
        }
    }

    fun toggleFavorite() {
        val content = currentContent() ?: return
        if (favoriteJob?.isActive == true) return
        val favorite = !content.isFavorite
        updateContent { it.copy(isFavorite = favorite) }
        favoriteJob = viewModelScope.launch {
            try {
                setPlaceFavorite(placeId, favorite)
            } catch (e: AppError) {
                // Removing a favorite the backend never had already gives the user the state they asked for.
                if (!favorite && e.code == AppError.FAVORITE_NOT_FOUND) return@launch
                updateContent { it.copy(isFavorite = !favorite) }
                _events.send(PlaceDetailEvent.FavoriteFailed)
            }
        }
    }

    fun submitReview(rating: Int, text: String) {
        if (submitJob?.isActive == true) return
        _reviewState.value = ReviewSubmitState.Submitting
        submitJob = viewModelScope.launch {
            try {
                submitPlaceReview(placeId, rating, text.trim().ifEmpty { null })
                _reviewState.value = ReviewSubmitState.Submitted
                refreshReviews()
            } catch (e: AppError) {
                _reviewState.value = ReviewSubmitState.Failed(e)
            }
        }
    }

    /** Called by the review sheet once it has reacted to [ReviewSubmitState.Submitted] or [ReviewSubmitState.Failed]. */
    fun acknowledgeReviewResult() {
        if (_reviewState.value is ReviewSubmitState.Submitted) _events.trySend(PlaceDetailEvent.ReviewSubmitted)
        if (_reviewState.value !is ReviewSubmitState.Submitting) _reviewState.value = ReviewSubmitState.Idle
    }

    private suspend fun refreshReviews() {
        val page = try {
            getPlaceReviews(placeId, FIRST_PAGE)
        } catch (e: AppError) {
            return
        }
        updateContent {
            it.copy(
                reviews = page.reviews,
                reviewTotal = page.total,
                averageRating = page.averageRating ?: it.detail.place.rating,
                hasMoreReviews = page.hasMore,
                isLoadingMoreReviews = false,
                nextReviewPage = page.page + 1,
            )
        }
    }

    private fun PlaceDetail.toContent(firstPage: ReviewPage?) = PlaceDetailContent(
        detail = this,
        isFavorite = isFavorite,
        reviews = firstPage?.reviews.orEmpty(),
        reviewTotal = firstPage?.total ?: 0,
        averageRating = firstPage?.averageRating ?: place.rating,
        hasMoreReviews = firstPage?.hasMore ?: false,
        isLoadingMoreReviews = false,
        nextReviewPage = (firstPage?.page ?: FIRST_PAGE) + 1,
    )

    private fun currentContent(): PlaceDetailContent? = (_uiState.value as? PlaceDetailUiState.Content)?.content

    private fun updateContent(transform: (PlaceDetailContent) -> PlaceDetailContent) {
        _uiState.update { state ->
            if (state is PlaceDetailUiState.Content) PlaceDetailUiState.Content(transform(state.content)) else state
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
