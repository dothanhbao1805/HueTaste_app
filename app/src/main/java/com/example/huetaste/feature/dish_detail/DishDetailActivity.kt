package com.example.huetaste.feature.dish_detail

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.annotation.ColorRes
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
import com.example.huetaste.R
import com.example.huetaste.common.extension.openMap
import com.example.huetaste.common.ui.gallery.GalleryPagerAdapter
import com.example.huetaste.common.ui.gallery.ImageViewerDialog
import com.example.huetaste.databinding.ActivityDishDetailBinding
import com.example.huetaste.databinding.ItemDishIngredientBinding
import com.example.huetaste.databinding.ItemDishPillBinding
import com.example.huetaste.databinding.ItemDishPlaceBinding
import com.example.huetaste.databinding.ItemDishSimilarBinding
import com.example.huetaste.domain.model.ScanStatus
import com.example.huetaste.feature.place_detail.PlaceDetailActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
    class DishDetailActivity : LsActivity<ActivityDishDetailBinding>(ActivityDishDetailBinding::inflate) {
    private val viewModel: DishDetailViewModel by viewModels()

    private var isFavorite = true
    // Once the user has a favorite state (restored or toggled), loaded data no longer overrides it.
    private var hasOwnFavorite = false
    private var content: DishDetailContent? = null
    // Hero page to return to after recreation; applied once the gallery has been submitted.
    private var pendingHeroPage: Int? = null
    private val heroAdapter = GalleryPagerAdapter(onImageClick = ::openImageViewer)
    private val heroPageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = showHeroCounter(position)
    }
    private val priceFormat = NumberFormat.getIntegerInstance(Locale("vi", "VN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let {
            isFavorite = it.getBoolean(KEY_FAVORITE, true)
            hasOwnFavorite = it.getBoolean(KEY_HAS_OWN_FAVORITE)
            pendingHeroPage = it.getInt(KEY_HERO_PAGE)
        }
        // viewCreated() runs inside LsActivity.onCreate, so restored state must be read first.
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }
        binding.shareButton.clicks { shareDish() }
        binding.retryButton.clicks { viewModel.load() }

        // The header and hero hearts show the same favorite state.
        showFavorite()
        binding.headerFavoriteButton.clicks { toggleFavorite() }
        binding.heroFavoriteButton.clicks { toggleFavorite() }

        binding.heroPager.adapter = heroAdapter
        binding.heroPager.registerOnPageChangeCallback(heroPageCallback)
        // The viewer reports the page it was closed on so the hero follows what the user last looked at.
        supportFragmentManager.setFragmentResultListener(ImageViewerDialog.RESULT_KEY, this) { _, result ->
            val page = result.getInt(ImageViewerDialog.RESULT_PAGE)
            if (page in 0 until heroAdapter.itemCount) binding.heroPager.setCurrentItem(page, false)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: DishDetailUiState) {
        binding.stateOverlay.isVisible = state is DishDetailUiState.Loading || state is DishDetailUiState.Error
        binding.loadingIndicator.isVisible = state is DishDetailUiState.Loading
        binding.retryButton.isVisible = state is DishDetailUiState.Error
        when (state) {
            DishDetailUiState.Sample -> Unit
            DishDetailUiState.Loading -> binding.stateMessage.setText(R.string.dish_loading)
            is DishDetailUiState.Error -> binding.stateMessage.text =
                state.error.message?.takeIf { it.isNotBlank() } ?: getString(R.string.dish_load_error)
            is DishDetailUiState.Content -> if (content != state.content) showContent(state.content)
        }
    }

    private fun showContent(dish: DishDetailContent) {
        content = dish
        if (!hasOwnFavorite) {
            isFavorite = dish.isFavorite
            showFavorite()
        }

        // Hero
        showHeroImages(dish)
        binding.heroBadge.isVisible = dish.categoryName != null
        binding.heroBadgeText.text = dish.categoryName
        binding.heroTagTime.isVisible = dish.mealTimes.isNotEmpty()
        binding.heroTagTimeText.text = dish.mealTimes.joinToString(", ")
        val firstTag = dish.tags.firstOrNull()
        binding.heroTagSpicy.isVisible = firstTag != null
        binding.heroTagSpicyText.text = firstTag
        binding.heroTagHeritage.isVisible = dish.isCatalogDish

        binding.statusBanner.isVisible = !dish.isCatalogDish
        binding.statusBannerText.setText(
            if (dish.scanStatus == ScanStatus.NOT_SAVED) R.string.dish_status_not_hue else R.string.dish_status_unverified,
        )

        // Name and price; the backend has no dish-level rating or English name.
        binding.dishNameText.text = dish.name
        binding.authenticBadge.isVisible = dish.isCatalogDish
        binding.englishNameText.visibility = View.GONE
        binding.ratingRow.visibility = View.INVISIBLE
        val price = formatPrice(dish.priceFrom, dish.priceTo)
        binding.priceBox.isVisible = price != null
        binding.priceText.text = price
        showPills(dish.tags.drop(1))

        binding.storyCard.isVisible = !dish.description.isNullOrBlank()
        binding.storyText.text = dish.description
        binding.storyTipBox.visibility = View.GONE

        showIngredients(dish)
        showPlaces(dish)
        showSimilarDishes(dish)

        // Sample-only sections without backend data yet.
        binding.reviewsCard.visibility = View.GONE
        binding.aiCard.visibility = View.GONE
        val firstPlace = dish.places.firstOrNull()?.place
        binding.directionsButton.isVisible = firstPlace != null
        binding.directionsButton.clicks { firstPlace?.let { openMap(it) } }
        binding.viewMapButton.clicks { firstPlace?.let { openMap(it) } }
    }

    private fun showHeroImages(dish: DishDetailContent) {
        val images = dish.images
        heroAdapter.submitList(images) {
            pendingHeroPage?.takeIf { it in 0 until heroAdapter.itemCount }
                ?.let { binding.heroPager.setCurrentItem(it, false) }
            pendingHeroPage = null
            showHeroCounter(binding.heroPager.currentItem)
        }
        // The sample photo only stays while the screen is opened without a dish.
        binding.heroPager.isVisible = images.isNotEmpty()
        binding.heroImage.isVisible = images.isEmpty()
        binding.heroImage.contentDescription = dish.name
        if (images.isEmpty()) binding.heroImage.setImageDrawable(null)
    }

    private fun showHeroCounter(position: Int) {
        val total = heroAdapter.itemCount
        binding.heroCounter.isVisible = total > 1
        binding.heroCounter.text = getString(R.string.dish_image_counter, position + 1, total)
    }

    private fun openImageViewer(position: Int) {
        val urls = heroAdapter.currentList.map { it.url }
        if (urls.isNotEmpty()) ImageViewerDialog.show(supportFragmentManager, urls, position)
    }

    private fun showPills(tags: List<String>) {
        val container = binding.pillsContainer
        // Keep the Flow helper; replace the sample pills it arranged.
        for (index in container.childCount - 1 downTo 0) {
            val child = container.getChildAt(index)
            if (child.id != R.id.pillsFlow) container.removeViewAt(index)
        }
        val ids = tags.map { tag ->
            val pill = ItemDishPillBinding.inflate(layoutInflater, container, false).root
            pill.id = View.generateViewId()
            pill.text = tag
            container.addView(pill)
            pill.id
        }
        binding.pillsFlow.referencedIds = ids.toIntArray()
        container.isVisible = ids.isNotEmpty()
    }

    private fun showIngredients(dish: DishDetailContent) {
        binding.ingredientsCard.isVisible = dish.ingredients.isNotEmpty()
        binding.nutritionText.visibility = View.GONE
        binding.ingredientsList.removeAllViews()
        dish.ingredients.sortedByDescending { it.isMain }.forEach { ingredient ->
            val item = ItemDishIngredientBinding.inflate(layoutInflater, binding.ingredientsList, true)
            item.ingredientName.text = ingredient.name
            item.ingredientGroup.isVisible = !ingredient.group.isNullOrBlank()
            item.ingredientGroup.text = ingredient.group
            if (ingredient.isMain) {
                item.ingredientRole.setText(R.string.dish_role_main)
            } else {
                item.ingredientIcon.setImageResource(R.drawable.ic_eco)
                item.ingredientIcon.imageTintList = colorList(R.color.hue_on_surface_variant)
                item.ingredientIconBackground.backgroundTintList = colorList(R.color.hue_surface_container_high)
                item.ingredientRole.setText(R.string.dish_role_side)
                item.ingredientRole.backgroundTintList = colorList(R.color.hue_surface_container_highest)
                item.ingredientRole.setTextColor(ContextCompat.getColor(this, R.color.hue_on_surface))
            }
        }
    }

    private fun showPlaces(dish: DishDetailContent) {
        binding.placesCard.isVisible = dish.places.isNotEmpty()
        binding.placesList.removeAllViews()
        dish.places.forEach { dishPlace ->
            val place = dishPlace.place
            val item = ItemDishPlaceBinding.inflate(layoutInflater, binding.placesList, true)
            item.placeImage.load(place.imageUrl) {
                placeholder(R.drawable.ic_food_placeholder)
                error(R.drawable.ic_food_placeholder)
            }
            item.placeName.text = place.name
            item.placeRatingIcon.isVisible = place.rating != null
            item.placeRating.isVisible = place.rating != null
            item.placeRating.text = place.rating?.let { String.format(Locale.US, "%.1f", it) }
            item.placeAddress.isVisible = !place.address.isNullOrBlank()
            item.placeAddress.text = place.address
            val price = (dishPlace.price ?: place.priceLevel)?.let { formatPrice(it, null) }
            item.placePrice.isVisible = price != null
            item.placePrice.text = price
            item.placeHours.text = place.openingHours.joinToString(", ")
            item.placeSignature.isVisible = dishPlace.isSignature
            item.placeDirectionsButton.clicks { openMap(place) }
            item.root.clicks { startActivity(PlaceDetailActivity.newIntent(this, place.id)) }
        }
    }

    private fun showSimilarDishes(dish: DishDetailContent) {
        binding.similarCard.isVisible = dish.similarDishes.isNotEmpty()
        binding.similarList.removeAllViews()
        dish.similarDishes.forEach { similar ->
            val item = ItemDishSimilarBinding.inflate(layoutInflater, binding.similarList, true)
            item.similarImage.load(similar.imageUrl) {
                placeholder(R.drawable.ic_food_placeholder)
                error(R.drawable.ic_food_placeholder)
            }
            item.similarName.text = similar.name
            item.similarDescription.isVisible = !similar.description.isNullOrBlank()
            item.similarDescription.text = similar.description
            item.root.clicks { startActivity(newIntent(this, similar.id)) }
        }
    }

    private fun formatPrice(from: Int?, to: Int?): String? = when {
        from != null && to != null && from != to ->
            getString(R.string.dish_price_range, priceFormat.format(from), priceFormat.format(to))
        from != null -> getString(R.string.dish_price_value, priceFormat.format(from))
        to != null -> getString(R.string.dish_price_value, priceFormat.format(to))
        else -> null
    }

    private fun colorList(@ColorRes color: Int) = ColorStateList.valueOf(ContextCompat.getColor(this, color))

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        hasOwnFavorite = true
        showFavorite()
    }

    private fun showFavorite() {
        binding.headerFavoriteButton.isSelected = isFavorite
        binding.heroFavoriteButton.isSelected = isFavorite
    }

    private fun shareDish() {
        val dish = content
        val text = if (dish != null) {
            listOfNotNull(dish.name, dish.description?.takeIf { it.isNotBlank() }).joinToString(" – ")
        } else {
            getString(R.string.dish_share_text, getString(R.string.dish_name), getString(R.string.dish_story_tip))
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, getString(R.string.dish_share_chooser)))
    }

    override fun onDestroy() {
        binding.heroPager.unregisterOnPageChangeCallback(heroPageCallback)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FAVORITE, isFavorite)
        outState.putBoolean(KEY_HAS_OWN_FAVORITE, hasOwnFavorite || content != null)
        outState.putInt(KEY_HERO_PAGE, binding.heroPager.currentItem)
    }

    companion object {
        const val EXTRA_SCAN_ID = "scan_id"
        const val EXTRA_DISH_ID = "dish_id"
        private const val KEY_FAVORITE = "dish_favorite"
        private const val KEY_HAS_OWN_FAVORITE = "dish_has_own_favorite"
        private const val KEY_HERO_PAGE = "dish_hero_page"

        /** Opens the sample dish shown by the static context suggestion screen. */
        fun newIntent(context: Context): Intent = Intent(context, DishDetailActivity::class.java)

        fun newIntent(context: Context, dishId: String): Intent =
            newIntent(context).putExtra(EXTRA_DISH_ID, dishId)

        fun newScanIntent(context: Context, scanId: String): Intent =
            newIntent(context).putExtra(EXTRA_SCAN_ID, scanId)
    }
}
