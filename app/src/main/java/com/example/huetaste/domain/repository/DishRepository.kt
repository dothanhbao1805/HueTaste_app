package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.DishCategory
import com.example.huetaste.domain.model.DishDetail
import com.example.huetaste.domain.model.DishSearchItem
import com.example.huetaste.domain.model.DishSearchQuery
import com.example.huetaste.domain.model.SearchPage

interface DishRepository {
    suspend fun getDishDetail(dishId: String): DishDetail

    suspend fun getCategories(): List<DishCategory>

    /** The first [size] dishes of the catalog, which needs no account. */
    suspend fun getDishes(size: Int): List<Dish>

    suspend fun searchDishes(query: DishSearchQuery, page: Int): SearchPage<DishSearchItem>

    suspend fun getFavoriteDishIds(): Set<String>

    suspend fun setFavorite(dishId: String, favorite: Boolean)
}
