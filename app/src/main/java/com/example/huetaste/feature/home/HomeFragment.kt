package com.example.huetaste.feature.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.huetaste.common.extension.startPlaceDetailActivity
import com.example.huetaste.common.extension.startScanActivity
import com.example.huetaste.databinding.FragmentHomeBinding
import com.example.huetaste.domain.model.AppError
import com.example.huetaste.domain.model.DeviceArea
import com.example.huetaste.domain.model.HomePlacesKind
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

/** The greeting, suggested dishes and places come from the backend; the places follow the device's location. */
@AndroidEntryPoint
class HomeFragment : LsFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {
    private val viewModel by lazy { ViewModelProvider(this)[HomeViewModel::class.java] }

    private val dishAdapter = HomeDishAdapter(
        onDishClick = { item -> requireActivity().startDishDetailActivity(item.dish.id) },
        onFavoriteClick = { item -> viewModel.toggleFavorite(item.dish.id) },
    )
    private val categoryAdapter = HomeCategoryAdapter()
    private val placeAdapter = HomePlaceAdapter(
        onPlaceClick = { item -> requireActivity().startPlaceDetailActivity(item.place.id) },
    )

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

    /** A first tap always shows the system dialog; only after that can "no dialog" mean it was denied for good. */
    private var hasRequestedLocation = false

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        viewModel.refreshLocation(openSettingsIfOff = result.values.any { it })
    }

    private val locationSettings = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshLocation()
    }

    override fun onViewCreated() {
        binding.homeDishList.adapter = dishAdapter
        binding.homeCategoryList.adapter = categoryAdapter
        binding.homePlaceList.adapter = placeAdapter

        mealChips.forEach { (meal, chip) -> chip.clicks { viewModel.selectMeal(meal) } }
        showBudget(binding.budgetSlider.value)
        binding.budgetSlider.addOnChangeListener(budgetListener)

        binding.suggestButton.clicks { suggest() }
        binding.homeDishesRetry.clicks { viewModel.retry() }
        binding.homePlacesRetry.clicks { viewModel.retryPlaces() }
        binding.scanDishButton.clicks { requireContext().startScanActivity() }
        binding.locationGroup.clicks { onLocationClicked() }

        // These entries have no destination yet; they still give the standard ripple and press animation.
        with(binding) {
            listOf(notificationsButton, seeAllDishes, mapLink).forEach { it.clicksComingSoon() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.onScreenStarted()
                launch { viewModel.uiState.collect(::render) }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            HomeEvent.FavoriteFailed -> requireContext().makeToast(R.string.home_favorite_failed)
                            HomeEvent.AskLocationPermission -> requestLocationPermission()
                            HomeEvent.OpenLocationSettings -> openLocationSettings()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: HomeUiState) {
        val dayPart = currentDayPart()
        showLocation(state.location)
        showGreeting(state.user, dayPart)
        showContext(state.context, dayPart)
        showSubtitle(state)
        mealChips.forEach { (meal, chip) -> chip.isSelected = meal == state.selectedMeal }
        showDishes(state)
        showPlaces(state)

        binding.categoriesSection.isVisible = state.categories.isNotEmpty()
        binding.categoriesCaption.text = getString(R.string.home_categories_count, state.categories.size)
        categoryAdapter.submitList(state.categories)
    }

    private fun showLocation(location: DeviceArea?) {
        when (location) {
            null -> {
                binding.locationTitle.setText(R.string.home_location_loading)
                binding.locationSubtitle.setText(R.string.home_location_subtitle)
            }
            DeviceArea.PermissionMissing, DeviceArea.LocationOff -> {
                binding.locationTitle.setText(R.string.home_location_off)
                binding.locationSubtitle.setText(R.string.home_location_off_subtitle)
            }
            is DeviceArea.Found -> {
                binding.locationTitle.text = location.name ?: getString(R.string.home_location_on)
                binding.locationSubtitle.setText(
                    if (location.name != null) R.string.home_location_subtitle else R.string.home_location_unknown_subtitle,
                )
            }
        }
    }

    private fun onLocationClicked() {
        when (viewModel.uiState.value.location) {
            null -> Unit
            DeviceArea.PermissionMissing -> {
                val deniedForGood = hasRequestedLocation &&
                    LOCATION_PERMISSIONS.none { shouldShowRequestPermissionRationale(it) }
                if (deniedForGood) {
                    openAppSettings()
                } else {
                    hasRequestedLocation = true
                    requestLocationPermission()
                }
            }
            DeviceArea.LocationOff -> openLocationSettings()
            is DeviceArea.Found -> viewModel.refreshLocation()
        }
    }

    private fun requestLocationPermission() {
        locationPermission.launch(LOCATION_PERMISSIONS)
    }

    private fun openLocationSettings() {
        locationSettings.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
    }

    private fun openAppSettings() {
        val packageUri = Uri.fromParts("package", requireContext().packageName, null)
        locationSettings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
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

    private fun showPlaces(state: HomeUiState) {
        val places = state.places
        val content = places as? HomePlacesState.Content
        val items = state.placeItems
        binding.homePlaceList.isVisible = items.isNotEmpty()
        binding.homePlacesLoading.isVisible = places is HomePlacesState.Loading
        binding.homePlacesMessage.isVisible = places is HomePlacesState.Error || (content != null && items.isEmpty())
        binding.homePlacesRetry.isVisible = places is HomePlacesState.Error
        if (places is HomePlacesState.Error) {
            binding.homePlacesMessageText.setText(errorMessage(places.error, R.string.home_places_error))
        } else if (content != null && items.isEmpty()) {
            binding.homePlacesMessageText.setText(R.string.home_places_empty)
        }
        binding.placesTitle.setText(
            when (content?.places?.kind) {
                HomePlacesKind.NEARBY -> R.string.home_nearby_title
                HomePlacesKind.FEATURED -> R.string.home_featured_title
                null -> R.string.home_places_title
            },
        )
        val subtitle = content?.let(::placesSubtitle)
        binding.placesSubtitle.isVisible = subtitle != null
        binding.placesSubtitle.text = subtitle
        placeAdapter.submitList(items)
    }

    private fun placesSubtitle(content: HomePlacesState.Content): String? = when {
        content.places.kind == HomePlacesKind.NEARBY ->
            (content.area as? DeviceArea.Found)?.name?.let { getString(R.string.home_nearby_subtitle, it) }
        content.area is DeviceArea.Found -> null
        else -> getString(R.string.home_featured_enable_location)
    }

    @StringRes
    private fun errorMessage(error: AppError, @StringRes otherwise: Int = R.string.home_suggestions_error): Int =
        if (error.code == AppError.NETWORK) R.string.ai_chat_error_network else otherwise

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
        binding.homePlaceList.adapter = null
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
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
