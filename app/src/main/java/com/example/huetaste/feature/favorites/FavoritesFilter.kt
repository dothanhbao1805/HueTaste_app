package com.example.huetaste.feature.favorites

import com.example.huetaste.common.extension.isOpenNow
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.Place
import com.example.huetaste.domain.model.distanceKmTo
import java.text.Normalizer
import kotlin.random.Random

enum class FavoritesTab { DISHES, PLACES }

/** [code] is the backend `buoiPhuHop` value; ALL keeps every dish. */
enum class MealFilter(val code: String?) {
    ALL(null),
    MORNING("sang"),
    NOON("trua"),
    AFTERNOON("chieu"),
    EVENING("toi"),
}

enum class PlaceFilter { ALL, OPEN_NOW }

enum class DishSort { RECENT, PRICE_LOW, PRICE_HIGH, NAME }

enum class PlaceSort { RECENT, NEAREST, RATING }

/** One dish card: the backend sends only the category id, so the name is joined from the category list. */
data class FavoriteDishItem(val dish: Dish, val categoryName: String?)

/** One place card. [isOpen] is null when the opening hours cannot be read; [distanceKm] when the position is unknown. */
data class FavoritePlaceItem(val place: Place, val isOpen: Boolean?, val distanceKm: Double?)

/**
 * Filtering and sorting of the saved lists. Both lists arrive newest first, and every sort is stable, so
 * [DishSort.RECENT] and [PlaceSort.RECENT] simply keep the incoming order.
 */
object FavoritesFilter {

    fun dishes(
        dishes: List<Dish>,
        categoryNames: Map<String, String>,
        meal: MealFilter,
        sort: DishSort,
        query: String,
    ): List<FavoriteDishItem> {
        val needle = query.fold()
        val filtered = dishes.filter { dish ->
            (meal.code == null || meal.code in dish.mealTimes) &&
                (needle.isEmpty() || dish.name.fold().contains(needle))
        }
        val sorted = when (sort) {
            DishSort.RECENT -> filtered
            // Dishes with no price go last in either direction.
            DishSort.PRICE_LOW -> filtered.sortedWith(compareBy(nullsLast<Int>()) { it.priceFrom })
            DishSort.PRICE_HIGH -> {
                val (priced, unpriced) = filtered.partition { (it.priceTo ?: it.priceFrom) != null }
                priced.sortedByDescending { it.priceTo ?: it.priceFrom } + unpriced
            }
            DishSort.NAME -> filtered.sortedBy { it.name.fold() }
        }
        return sorted.map { FavoriteDishItem(it, it.categoryId?.let(categoryNames::get)) }
    }

    fun places(
        places: List<Place>,
        position: GeoPoint?,
        filter: PlaceFilter,
        sort: PlaceSort,
        query: String,
    ): List<FavoritePlaceItem> {
        val needle = query.fold()
        val items = places
            .filter { place ->
                needle.isEmpty() || place.name.fold().contains(needle) || place.address.orEmpty().fold().contains(needle)
            }
            .map { place ->
                FavoritePlaceItem(place, place.isOpenNow(), distanceTo(position, place))
            }
            .filter { filter == PlaceFilter.ALL || it.isOpen == true }
        return when (sort) {
            PlaceSort.RECENT -> items
            PlaceSort.NEAREST -> items.sortedWith(compareBy(nullsLast<Double>()) { it.distanceKm })
            PlaceSort.RATING -> {
                val (rated, unrated) = items.partition { it.place.rating != null }
                rated.sortedByDescending { it.place.rating } + unrated
            }
        }
    }

    /** The backend `buoiPhuHop` code of the meal at [hour], on the same hours as the home screen and the backend. */
    fun mealCodeForHour(hour: Int): String = when (hour) {
        in 5..9 -> "sang"
        in 10..13 -> "trua"
        in 14..16 -> "chieu"
        else -> "toi"
    }

    /**
     * A random saved dish for the meal [mealCode]; with none for that meal, any saved dish. The dish picked last
     * time ([exclude]) is skipped when there is another choice, so pressing the button again changes the answer.
     */
    fun pickRandom(dishes: List<Dish>, mealCode: String, exclude: String?, random: Random): Dish? {
        val forMeal = dishes.filter { mealCode in it.mealTimes }.ifEmpty { dishes }
        val choices = forMeal.filter { it.id != exclude }.ifEmpty { forMeal }
        return choices.randomOrNull(random)
    }

    private fun distanceTo(position: GeoPoint?, place: Place): Double? {
        val latitude = place.latitude ?: return null
        val longitude = place.longitude ?: return null
        return position?.distanceKmTo(GeoPoint(latitude, longitude))
    }

    /** Lower case without accents, so "bun bo" finds "Bún bò". */
    private fun String.fold(): String =
        Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .replace('đ', 'd')

    private val COMBINING_MARKS = Regex("\\p{InCombiningDiacriticalMarks}+")
}
