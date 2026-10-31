package com.example.huetaste.feature.place_detail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import coil.load
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.openMap
import com.example.huetaste.common.ui.gallery.GalleryPagerAdapter
import com.example.huetaste.common.ui.gallery.ImageViewerDialog
import com.example.huetaste.databinding.ActivityPlaceDetailBinding
import com.example.huetaste.databinding.ItemPlaceMenuDishBinding
import com.example.huetaste.databinding.ItemPlaceReviewBinding
import com.example.huetaste.domain.model.OpeningHours
import com.example.huetaste.domain.model.PlaceDetail
import com.example.huetaste.domain.model.PlaceMenuItem
import com.example.huetaste.domain.model.Review
import com.example.huetaste.feature.dish_detail.DishDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class PlaceDetailActivity : LsActivity<ActivityPlaceDetailBinding>(ActivityPlaceDetailBinding::inflate) {
    private val viewModel: PlaceDetailViewModel by viewModels()

    private val priceFormat = NumberFormat.getIntegerInstance(Locale("vi", "VN"))
    private val heroAdapter = GalleryPagerAdapter(onImageClick = ::openImageViewer)
    private val heroPageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = showHeroCounter(position)
    }

    // The place and reviews last drawn, so a review or favorite update does not rebuild the whole screen.
    private var shownDetail: PlaceDetail? = null
    private var shownReviews: List<Review>? = null
    private var currentPlace: PlaceDetail? = null

    // Hero page to return to after recreation; applied once the gallery has been submitted.
    private var pendingHeroPage: Int? = null

    private val stars get() = with(binding) {
        listOf(overviewStar1, overviewStar2, overviewStar3, overviewStar4, overviewStar5)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        pendingHeroPage = savedInstanceState?.getInt(KEY_HERO_PAGE)
        // viewCreated() runs inside LsActivity.onCreate, so restored state must be read first.
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }
        binding.shareButton.clicks { sharePlace() }
        binding.retryButton.clicks { viewModel.load() }

        binding.headerFavoriteButton.clicks { viewModel.toggleFavorite() }
        binding.actionSave.clicks { viewModel.toggleFavorite() }
        binding.directionsButton.clicks { currentPlace?.let { openMap(it.place) } }
        binding.actionDirections.clicks { currentPlace?.let { openMap(it.place) } }
        binding.openMapButton.clicks { currentPlace?.let { openMap(it.place) } }
        binding.actionLocation.clicks { binding.placeScroll.smoothScrollTo(0, binding.mapSection.top) }
        binding.actionCall.clicks { currentPlace?.phone?.let(::dial) }
        binding.callNowButton.clicks { currentPlace?.phone?.let(::dial) }
        binding.writeReviewButton.clicks { ReviewSheet.show(supportFragmentManager) }
        binding.loadMoreReviewsButton.clicks { viewModel.loadMoreReviews() }

        binding.heroPager.adapter = heroAdapter
        binding.heroPager.registerOnPageChangeCallback(heroPageCallback)
        // The viewer reports the page it was closed on so the hero follows what the user last looked at.
        supportFragmentManager.setFragmentResultListener(ImageViewerDialog.RESULT_KEY, this) { _, result ->
            val page = result.getInt(ImageViewerDialog.RESULT_PAGE)
            if (page in 0 until heroAdapter.itemCount) binding.heroPager.setCurrentItem(page, false)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch { viewModel.events.collect(::handle) }
                launch { viewModel.reviewState.collect(::acknowledgeWhenSheetClosed) }
            }
        }
    }

    private fun render(state: PlaceDetailUiState) {
        binding.stateOverlay.isVisible = state !is PlaceDetailUiState.Content
        binding.loadingIndicator.isVisible = state is PlaceDetailUiState.Loading
        binding.retryButton.isVisible = state is PlaceDetailUiState.Error
        when (state) {
            PlaceDetailUiState.Loading -> binding.stateMessage.setText(R.string.place_loading)
            is PlaceDetailUiState.Error -> binding.stateMessage.text =
                state.error.message?.takeIf { it.isNotBlank() } ?: getString(R.string.place_load_error)
            is PlaceDetailUiState.Content -> showContent(state.content)
        }
    }

    private fun handle(event: PlaceDetailEvent) {
        when (event) {
            PlaceDetailEvent.FavoriteFailed -> makeToast(R.string.place_favorite_failed)
            PlaceDetailEvent.ReviewSubmitted -> makeToast(R.string.place_review_submitted)
        }
    }

    /** A review can finish after the sheet was closed; nobody else would report its result then. */
    private fun acknowledgeWhenSheetClosed(state: ReviewSubmitState) {
        val finished = state is ReviewSubmitState.Submitted || state is ReviewSubmitState.Failed
        if (finished && !ReviewSheet.isShown(supportFragmentManager)) viewModel.acknowledgeReviewResult()
    }

    private fun showContent(content: PlaceDetailContent) {
        val detail = content.detail
        currentPlace = detail
        if (shownDetail !== detail) {
            shownDetail = detail
            showDetail(detail)
        }
        showFavorite(content.isFavorite)
        showReviews(content)
    }

    private fun showDetail(detail: PlaceDetail) {
        val place = detail.place
        showHeroImages(detail)
        showOpenStatus(detail)

        binding.placeName.text = place.name
        showMeta(detail)

        binding.addressRow.isVisible = !place.address.isNullOrBlank()
        binding.placeAddress.text = place.address
        showHours(detail)
        binding.phoneRow.isVisible = detail.phone != null
        binding.placePhone.text = detail.phone
        binding.actionCall.isVisible = detail.phone != null
        binding.infoCard.isVisible = binding.addressRow.isVisible || binding.hoursRow.isVisible || binding.phoneRow.isVisible

        val hasLocation = !place.address.isNullOrBlank() || (place.latitude != null && place.longitude != null)
        binding.mapSection.isVisible = hasLocation
        binding.actionLocation.isVisible = hasLocation
        binding.mapAddress.text = place.address ?: place.name

        showMenu(detail)
    }

    private fun showHeroImages(detail: PlaceDetail) {
        val images = detail.images
        heroAdapter.submitList(images) {
            pendingHeroPage?.takeIf { it in 0 until heroAdapter.itemCount }
                ?.let { binding.heroPager.setCurrentItem(it, false) }
            pendingHeroPage = null
            showHeroCounter(binding.heroPager.currentItem)
        }
        binding.heroPager.isVisible = images.isNotEmpty()
        binding.heroPlaceholder.isVisible = images.isEmpty()
        binding.heroPlaceholder.contentDescription = detail.place.name
    }

    private fun showHeroCounter(position: Int) {
        val total = heroAdapter.itemCount
        binding.heroCounterPill.isVisible = total > 1
        binding.heroCounter.text = getString(R.string.dish_image_counter, position + 1, total)
    }

    private fun openImageViewer(position: Int) {
        val urls = heroAdapter.currentList.map { it.url }
        if (urls.isNotEmpty()) ImageViewerDialog.show(supportFragmentManager, urls, position)
    }

    /** Open or closed now, from today's line of the opening hours; hidden when the hours cannot be read. */
    private fun showOpenStatus(detail: PlaceDetail) {
        val today = todayHours(detail)
        val isOpen = today?.isOpenNow
        binding.heroStatus.isVisible = isOpen != null
        if (today == null || isOpen == null) return
        val color = if (isOpen) R.color.place_open else R.color.place_closed
        val textColor = if (isOpen) R.color.place_open_text else R.color.place_closed
        binding.heroStatusDot.backgroundTintList = colorList(color)
        binding.heroStatusText.setText(if (isOpen) R.string.place_open_now else R.string.place_closed_now)
        binding.heroStatusText.setTextColor(ContextCompat.getColor(this, textColor))
        binding.heroStatusHours.text = getString(R.string.place_hours_bullet, today.text)
    }

    private fun showHours(detail: PlaceDetail) {
        val lines = detail.place.openingHours
        binding.hoursRow.isVisible = lines.isNotEmpty()
        val today = todayHours(detail)
        binding.placeHoursToday.text = today?.let { getString(R.string.place_hours_today, it.text) } ?: lines.firstOrNull()
        binding.placeHoursAll.isVisible = lines.size > 1
        binding.placeHoursAll.text = lines.joinToString("\n")
    }

    private fun todayHours(detail: PlaceDetail) = Calendar.getInstance().let { now ->
        // Calendar counts Sunday as 1; OpeningHours counts Monday as 1 and Sunday as 7.
        val day = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        OpeningHours.forDay(
            detail.place.openingHours,
            day,
            now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE),
        )
    }

    /** Price range from what the place charges, then its price level as repeated currency signs. */
    private fun showMeta(detail: PlaceDetail) {
        val prices = detail.menu.mapNotNull { it.price }
        val range = when {
            prices.isEmpty() -> null
            prices.min() == prices.max() -> getString(R.string.dish_price_value, priceFormat.format(prices.min()))
            else -> getString(R.string.dish_price_range, priceFormat.format(prices.min()), priceFormat.format(prices.max()))
        }
        val level = detail.place.priceLevel?.takeIf { it > 0 }?.let { CURRENCY_SIGN.repeat(it.coerceAtMost(MAX_PRICE_LEVEL)) }
        val parts = listOfNotNull(range, level)
        binding.placeMeta.isVisible = parts.isNotEmpty()
        binding.placeMeta.text = parts.joinToString(" • ")
    }

    private fun showMenu(detail: PlaceDetail) {
        binding.menuSection.isVisible = detail.menu.isNotEmpty()
        binding.menuList.removeAllViews()
        detail.menu.forEach { menuItem ->
            val item = ItemPlaceMenuDishBinding.inflate(layoutInflater, binding.menuList, true)
            bindMenuItem(item, menuItem)
        }
    }

    private fun bindMenuItem(item: ItemPlaceMenuDishBinding, menuItem: PlaceMenuItem) {
        val dish = menuItem.dish
        item.menuDishImage.load(dish.imageUrl) {
            placeholder(R.drawable.ic_food_placeholder)
            error(R.drawable.ic_food_placeholder)
        }
        item.menuDishName.text = dish.name
        item.menuDishSignature.isVisible = menuItem.isSignature
        item.menuDishDescription.isVisible = !dish.description.isNullOrBlank()
        item.menuDishDescription.text = dish.description
        val price = menuItem.price ?: dish.priceFrom
        item.menuDishPrice.isVisible = price != null
        item.menuDishPrice.text = price?.let { getString(R.string.dish_price_value, priceFormat.format(it)) }
        item.root.clicks { startActivity(DishDetailActivity.newIntent(this, dish.id)) }
    }

    private fun showFavorite(favorite: Boolean) {
        binding.headerFavoriteButton.isSelected = favorite
        binding.actionSaveIcon.isSelected = favorite
        binding.actionSaveLabel.setText(if (favorite) R.string.place_saved else R.string.place_save)
    }

    private fun showReviews(content: PlaceDetailContent) {
        binding.reviewsSection.isVisible = true
        val average = content.averageRating
        binding.reviewsCount.isVisible = content.reviewTotal > 0
        binding.reviewsCount.text = getString(R.string.place_reviews_count, content.reviewTotal)

        binding.reviewOverview.isVisible = average != null
        if (average != null) {
            binding.reviewAverage.text = String.format(Locale.US, "%.1f", average)
            bindStars(stars, average)
            binding.reviewOverviewNote.setText(R.string.place_review_overview_note)
        }
        binding.heroRatingPill.isVisible = average != null
        if (average != null) {
            binding.heroRatingText.text = String.format(Locale.US, "%.1f", average)
            binding.heroRatingCount.isVisible = content.reviewTotal > 0
            binding.heroRatingCount.text = getString(R.string.place_rating_count, content.reviewTotal)
        }

        binding.reviewsEmpty.isVisible = content.reviews.isEmpty()
        if (shownReviews != content.reviews) {
            shownReviews = content.reviews
            binding.reviewsList.removeAllViews()
            content.reviews.forEach { review ->
                bindReview(ItemPlaceReviewBinding.inflate(layoutInflater, binding.reviewsList, true), review)
            }
        }
        binding.loadMoreReviewsButton.isVisible = content.hasMoreReviews
        binding.loadMoreReviewsButton.isEnabled = !content.isLoadingMoreReviews
        binding.loadMoreReviewsButton.alpha = if (content.isLoadingMoreReviews) DISABLED_ALPHA else 1f
    }

    private fun bindReview(item: ItemPlaceReviewBinding, review: Review) {
        val name = review.authorName?.takeIf { it.isNotBlank() } ?: getString(R.string.place_review_anonymous)
        item.reviewAuthor.text = name
        item.reviewAvatarInitial.text = name.first().uppercaseChar().toString()
        val hasAvatar = !review.authorAvatarUrl.isNullOrBlank()
        item.reviewAvatarImage.isVisible = hasAvatar
        item.reviewAvatarInitial.isVisible = !hasAvatar
        if (hasAvatar) {
            item.reviewAvatarImage.load(review.authorAvatarUrl) {
                placeholder(R.drawable.ic_food_placeholder)
                error(R.drawable.ic_food_placeholder)
            }
        }
        val date = formatDate(review.createdAt)
        item.reviewDate.isVisible = date != null
        item.reviewDate.text = date
        bindStars(
            with(item) { listOf(reviewStar1, reviewStar2, reviewStar3, reviewStar4, reviewStar5) },
            review.rating.toDouble(),
        )
        item.reviewContent.isVisible = review.content != null
        item.reviewContent.text = review.content
    }

    /** `2026-10-31T09:15:00` becomes `31/10/2026`; anything else is not shown. */
    private fun formatDate(iso: String?): String? {
        val parts = iso?.take(ISO_DATE_LENGTH)?.split("-") ?: return null
        if (parts.size != 3 || parts.any { it.toIntOrNull() == null }) return null
        return "${parts[2]}/${parts[1]}/${parts[0]}"
    }

    private fun dial(phone: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))))
        } catch (e: ActivityNotFoundException) {
            makeToast(R.string.place_no_phone_app)
        }
    }

    private fun sharePlace() {
        val detail = currentPlace ?: return
        val text = listOfNotNull(detail.place.name, detail.place.address?.takeIf { it.isNotBlank() }).joinToString(" – ")
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, getString(R.string.place_share_chooser)))
    }

    private fun colorList(color: Int) = ColorStateList.valueOf(ContextCompat.getColor(this, color))

    override fun onDestroy() {
        binding.heroPager.unregisterOnPageChangeCallback(heroPageCallback)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_HERO_PAGE, binding.heroPager.currentItem)
    }

    companion object {
        const val EXTRA_PLACE_ID = "place_id"
        private const val KEY_HERO_PAGE = "place_hero_page"
        private const val CURRENCY_SIGN = "₫"
        private const val MAX_PRICE_LEVEL = 4
        private const val ISO_DATE_LENGTH = 10
        private const val DISABLED_ALPHA = 0.5f

        fun newIntent(context: Context, placeId: String): Intent =
            Intent(context, PlaceDetailActivity::class.java).putExtra(EXTRA_PLACE_ID, placeId)
    }
}
