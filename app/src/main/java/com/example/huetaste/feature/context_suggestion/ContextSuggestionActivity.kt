package com.example.huetaste.feature.context_suggestion

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.startDishDetailActivity
import com.example.huetaste.common.extension.startPlaceDetailActivity
import com.example.huetaste.common.extension.weatherText
import com.example.huetaste.common.navigation.SuggestionArgs
import com.example.huetaste.databinding.ActivityContextSuggestionBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.feature.taste_setup.icon
import com.example.huetaste.feature.taste_setup.label
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** The suggestion results asked for from the review screen, in a dishes tab and a places tab. */
@AndroidEntryPoint
class ContextSuggestionActivity : LsActivity<ActivityContextSuggestionBinding>(ActivityContextSuggestionBinding::inflate) {
    private val viewModel: ContextSuggestionViewModel by viewModels()

    private val dishAdapter = SuggestedDishAdapter(
        onDishClick = { startDishDetailActivity(it.dish.id) },
        onFavoriteClick = { viewModel.toggleFavorite(it.dish.id) },
    )
    private val placeAdapter = SuggestedPlaceAdapter(
        onPlaceClick = { startPlaceDetailActivity(it.place.id) },
    )

    private val moneyFormat = NumberFormat.getIntegerInstance(Locale("vi", "VN"))

    override fun viewCreated() {
        binding.dishList.adapter = dishAdapter
        binding.placeList.adapter = placeAdapter

        binding.backButton.clicks { handleBackPressed() }
        // The review screen below is where the context is changed.
        binding.changeContextButton.clicks { finish() }
        binding.tabDishes.clicks { viewModel.selectTab(SuggestionTab.DISHES) }
        binding.tabPlaces.clicks { viewModel.selectTab(SuggestionTab.PLACES) }
        binding.retryButton.clicks { viewModel.retry() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            ContextSuggestionEvent.FavoriteFailed -> makeToast(R.string.home_favorite_failed)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: ContextSuggestionUiState) = with(binding) {
        showContext(state)

        val dishes = state.dishItems
        val places = state.placeItems
        val loaded = state.result != null
        tabDishesText.text = if (loaded) getString(R.string.context_tab_dishes_count, dishes.size) else getString(R.string.context_tab_dishes)
        tabPlacesText.text = if (loaded) getString(R.string.context_tab_places_count, places.size) else getString(R.string.context_tab_places)

        val onDishes = state.tab == SuggestionTab.DISHES
        tabDishes.isSelected = onDishes
        tabPlaces.isSelected = !onDishes
        dishAdapter.submitList(dishes)
        placeAdapter.submitList(places)

        val error = state.error
        val isEmpty = loaded && (if (onDishes) dishes.isEmpty() else places.isEmpty())
        loadingProgress.isVisible = state.isLoading
        dishList.isVisible = loaded && onDishes && dishes.isNotEmpty()
        placeList.isVisible = loaded && !onDishes && places.isNotEmpty()
        messageView.isVisible = !state.isLoading && (error != null || isEmpty)
        retryButton.isVisible = error != null
        when {
            error != null -> showMessage(
                icon = R.drawable.ic_cloud_off,
                text = if (error.code == AppError.NETWORK) R.string.ai_chat_error_network else R.string.home_suggestions_error,
                isError = true,
            )
            isEmpty && onDishes -> showMessage(R.drawable.ic_ramen_dining, R.string.context_dishes_empty, isError = false)
            isEmpty -> showMessage(R.drawable.ic_storefront, R.string.context_places_empty, isError = false)
        }
    }

    private fun showContext(state: ContextSuggestionUiState) = with(binding) {
        val args = state.args
        val meal = args.meal
        tagMeal.isVisible = meal != null
        if (meal != null) {
            tagMealIcon.setImageResource(meal.icon)
            tagMealText.setText(meal.label)
        }
        val budget = args.budget
        tagBudget.isVisible = budget != null
        if (budget != null) tagBudgetText.text = getString(R.string.review_budget_max, moneyFormat.format(budget))
        tagNearby.isVisible = args.point != null

        val context = state.result?.context
        val weather = weatherText(context?.weather)
        val temperature = context?.temperature?.roundToInt()
        tagWeather.isVisible = weather != null
        tagWeatherText.text = when {
            weather == null -> null
            temperature != null -> getString(R.string.home_context_weather, temperature, weather)
            else -> weather
        }
    }

    private fun showMessage(@DrawableRes icon: Int, @StringRes text: Int, isError: Boolean) = with(binding) {
        messageIcon.setImageResource(icon)
        messageIcon.backgroundTintList = ContextCompat.getColorStateList(
            this@ContextSuggestionActivity,
            if (isError) R.color.hue_error_container else R.color.hue_secondary_fixed,
        )
        messageIcon.imageTintList = ContextCompat.getColorStateList(
            this@ContextSuggestionActivity,
            if (isError) R.color.hue_error else R.color.hue_primary,
        )
        messageText.setText(text)
    }

    companion object {
        fun newIntent(context: Context, args: SuggestionArgs): Intent =
            args.putInto(Intent(context, ContextSuggestionActivity::class.java))
    }
}
