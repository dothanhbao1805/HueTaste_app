package com.example.huetaste.feature.favorites

import androidx.annotation.StringRes
import com.example.huetaste.R
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.model.Place

/**
 * Collections the screen builds by itself from what is saved: the backend has no user-made collections, so each
 * one is a rule over the saved dishes or places.
 */
enum class FavoriteCollection(val isDishes: Boolean, @StringRes val title: Int) {
    MORNING_DISHES(true, R.string.favorites_col_morning),
    RAINY_DISHES(true, R.string.favorites_col_rainy),
    HOT_DISHES(true, R.string.favorites_col_hot),
    OPEN_PLACES(false, R.string.favorites_col_open),
    NEARBY_PLACES(false, R.string.favorites_col_nearby),
}

/** What a collection card shows: [count] saved items and the photo of the first one that has a photo. */
data class CollectionSummary(val collection: FavoriteCollection, val count: Int, val coverUrl: String?)

object FavoriteCollections {
    /** A saved place within this many kilometers of the device belongs to the "near you" collection. */
    const val NEARBY_KM = 3.0

    fun dishes(collection: FavoriteCollection, all: List<Dish>): List<Dish> = when (collection) {
        FavoriteCollection.MORNING_DISHES -> all.filter { "sang" in it.mealTimes }
        FavoriteCollection.RAINY_DISHES -> all.filter { "mua" in it.weatherTags }
        FavoriteCollection.HOT_DISHES -> all.filter { "nong" in it.weatherTags }
        else -> emptyList()
    }

    /** The places of [collection] in the order of [sort]; the filter keeps that order. */
    fun places(
        collection: FavoriteCollection,
        all: List<Place>,
        position: GeoPoint?,
        sort: PlaceSort,
    ): List<FavoritePlaceItem> {
        val items = FavoritesFilter.places(all, position, PlaceFilter.ALL, sort, "")
        return when (collection) {
            FavoriteCollection.OPEN_PLACES -> items.filter { it.isOpen == true }
            FavoriteCollection.NEARBY_PLACES -> items.filter { (it.distanceKm ?: Double.MAX_VALUE) <= NEARBY_KM }
            else -> emptyList()
        }
    }

    /** The collections worth showing: the ones with at least one item. */
    fun summaries(
        isDishes: Boolean,
        dishes: List<Dish>,
        places: List<Place>,
        position: GeoPoint?,
    ): List<CollectionSummary> = FavoriteCollection.entries
        .filter { it.isDishes == isDishes }
        .mapNotNull { collection ->
            if (isDishes) {
                val items = dishes(collection, dishes)
                items.takeIf { it.isNotEmpty() }
                    ?.let { CollectionSummary(collection, it.size, it.firstNotNullOfOrNull { dish -> dish.imageUrl }) }
            } else {
                val items = places(collection, places, position, PlaceSort.RECENT)
                items.takeIf { it.isNotEmpty() }
                    ?.let { CollectionSummary(collection, it.size, it.firstNotNullOfOrNull { item -> item.place.imageUrl }) }
            }
        }
}
