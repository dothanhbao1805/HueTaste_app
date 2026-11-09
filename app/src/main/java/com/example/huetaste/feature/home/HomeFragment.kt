package com.example.huetaste.feature.home

import androidx.annotation.StringRes
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsFragment
import com.base.common.extension.clicks
import com.base.common.extension.makeToast
import com.example.huetaste.R
import com.example.huetaste.common.extension.clicksComingSoon
import com.example.huetaste.common.extension.loadWithShimmer
import com.example.huetaste.common.extension.startDishDetailActivity
import com.example.huetaste.common.extension.startLoginActivity
import com.example.huetaste.common.extension.startScanActivity
import com.example.huetaste.databinding.FragmentHomeBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.MealType
import com.example.huetaste.domain.model.SuggestionContext
import com.example.huetaste.domain.model.UserProfile
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/** The greeting and suggested dishes come from the backend; "Địa điểm gần bạn" is still the sample from the layout. */
@AndroidEntryPoint
class HomeFragment : LsFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(this)[HomeViewModel::class.java] }

    private val dishAdapter = HomeDishAdapter(
        onDishClick = { item -> requireActivity().startDishDetailActivity(item.dish.id) },
        onFavoriteClick = { item -> viewModel.toggleFavorite(item.dish.id) },
    )
    private val categoryAdapter = HomeCategoryAdapter()

    private val budgetFormat = NumberFormat.getIntegerInstance(Locale("vi", "VN"))

    private val mealChips get() = with(binding) {
        listOf(
            MealType.BREAKFAST to mealBreakfast,
            MealType.LUNCH to mealLunch,
            MealType.DINNER to mealDinner,
            MealType.SNACK to mealSnacks,
        )
    }

    private val budgetListener = Slider.OnChangeListener { _, value, _ -> showBudget(value) }

    /** The next list to arrive answers the suggest button, so it starts from its first card. */
    private var scrollDishesToStart = false

    private var shownUser: UserProfile? = null

    override fun onViewCreated() {
        binding.homeDishList.adapter = dishAdapter
        binding.homeCategoryList.adapter = categoryAdapter

        mealChips.forEach { (meal, chip) -> chip.clicks { viewModel.selectMeal(meal) } }
        showBudget(binding.budgetSlider.value)
        binding.budgetSlider.addOnChangeListener(budgetListener)

        binding.suggestButton.clicks { suggest() }
        binding.homeDishesRetry.clicks { viewModel.retry() }
        binding.scanDishButton.clicks { requireContext().startScanActivity() }

        // These entries have no destination yet; they still give the standard ripple and press animation.
        with(binding) {
            listOf(notificationsButton, seeAllDishes, mapLink, placeTuyet, placeHanh).forEach { it.clicksComingSoon() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            HomeEvent.FavoriteFailed -> requireContext().makeToast(R.string.home_favorite_failed)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: HomeUiState) {
        val dayPart = currentDayPart()
        showGreeting(state.user, dayPart)
        showContext(state.context, dayPart)
        showSubtitle(state)
        mealChips.forEach { (meal, chip) -> chip.isSelected = meal == state.selectedMeal }
        showDishes(state)

        binding.categoriesSection.isVisible = state.categories.isNotEmpty()
        binding.categoriesCaption.text = getString(R.string.home_categories_count, state.categories.size)
        categoryAdapter.submitList(state.categories)
    }

    private fun showGreeting(user: UserProfile?, dayPart: DayPart) {
        val greeting = getString(dayPart.greeting)
        binding.greetingTitle.text = user?.name?.takeIf { it.isNotBlank() }
            ?.let { getString(R.string.home_greeting_named, greeting, it) }
            ?: getString(R.string.home_greeting_guest, greeting)
        binding.sunBadge.isVisible = dayPart != DayPart.EVENING
        // Every state change renders; the photo only loads again for another user. A guest keeps the layout's icon.
        if (user != null && user != shownUser) {
            shownUser = user
            binding.profileAvatar.loadWithShimmer(user.avatarUrl) {
                error(R.drawable.ic_person)
                fallback(R.drawable.ic_person)
            }
        }
    }

    private fun showContext(context: SuggestionContext?, dayPart: DayPart) {
        val weather = weatherText(context?.weather)
        val temperature = context?.temperature?.roundToInt()
        binding.contextSummary.text = buildSpannedString {
            bold { append(getString(dayPart.label)) }
            if (temperature != null && weather != null) {
                append(SEPARATOR)
                append(getString(R.string.home_context_weather, temperature, weather))
            }
        }
    }

    private fun showSubtitle(state: HomeUiState) {
        val weather = weatherText(state.context?.weather)
        binding.suggestedSubtitle.text = when {
            !state.isSignedIn -> getString(R.string.home_suggested_guest)
            weather != null -> getString(R.string.home_suggested_weather, weather)
            else -> getString(R.string.home_suggested_taste)
        }
    }

    private fun showDishes(state: HomeUiState) {
        val dishes = state.dishes
        binding.homeDishList.isVisible = dishes is HomeDishesState.Content
        binding.homeDishesLoading.isVisible = dishes is HomeDishesState.Loading
        binding.homeDishesMessage.isVisible = dishes is HomeDishesState.Empty || dishes is HomeDishesState.Error
        binding.homeDishesRetry.isVisible = dishes is HomeDishesState.Error
        when (dishes) {
            HomeDishesState.Empty -> binding.homeDishesMessageText.setText(R.string.home_suggestions_empty)
            is HomeDishesState.Error -> binding.homeDishesMessageText.setText(errorMessage(dishes.error))
            else -> Unit
        }
        val toStart = scrollDishesToStart && dishes is HomeDishesState.Content
        if (toStart) scrollDishesToStart = false
        dishAdapter.submitList(state.dishItems) {
            if (toStart) binding.homeDishList.scrollToPosition(0)
        }
    }

    @StringRes
    private fun errorMessage(error: AppError): Int =
        if (error.code == AppError.NETWORK) R.string.ai_chat_error_network else R.string.home_suggestions_error

    private fun suggest() {
        val asked = viewModel.suggest(
            budget = binding.budgetSlider.value.toInt(),
            note = binding.notesInput.text?.toString().orEmpty(),
        )
        if (!asked) {
            requireContext().makeToast(R.string.home_suggest_sign_in)
            requireActivity().startLoginActivity()
            return
        }
        binding.notesInput.clearFocus()
        scrollDishesToStart = true
        binding.homeScroll.smoothScrollTo(0, binding.suggestedSection.top)
    }

    /** The backend codes of `NguCanh.thoiTiet`; anything else shows no weather. */
    private fun weatherText(code: String?): String? = when (code) {
        "nong" -> getString(R.string.home_weather_hot)
        "mat" -> getString(R.string.home_weather_cool)
        "lanh" -> getString(R.string.home_weather_cold)
        "mua" -> getString(R.string.home_weather_rain)
        else -> null
    }

    private fun currentDayPart(): DayPart = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 5..9 -> DayPart.MORNING
        in 10..13 -> DayPart.NOON
        in 14..16 -> DayPart.AFTERNOON
        else -> DayPart.EVENING
    }

    private fun showBudget(value: Float) {
        binding.budgetValue.text = getString(R.string.home_budget_format, budgetFormat.format(value.toLong()))
    }

    override fun onDestroyView() {
        binding.budgetSlider.removeOnChangeListener(budgetListener)
        binding.homeDishList.adapter = null
        binding.homeCategoryList.adapter = null
        shownUser = null
        super.onDestroyView()
    }

    private enum class DayPart(@StringRes val greeting: Int, @StringRes val label: Int) {
        MORNING(R.string.home_greeting_morning, R.string.home_part_morning),
        NOON(R.string.home_greeting_noon, R.string.home_part_noon),
        AFTERNOON(R.string.home_greeting_afternoon, R.string.home_part_afternoon),
        EVENING(R.string.home_greeting_evening, R.string.home_part_evening),
    }

    private companion object {
        const val SEPARATOR = " • "
    }
}
