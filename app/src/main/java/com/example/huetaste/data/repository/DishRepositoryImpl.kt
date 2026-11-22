package com.example.huetaste.data.repository

import com.example.huetaste.data.mapper.toDomain
import com.example.huetaste.data.mapper.toSearchItem
import com.example.huetaste.data.remote.HueTasteApi
import com.example.huetaste.data.remote.apiCall
import com.example.huetaste.data.remote.dto.YeuThichMonAnBody
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.Dish
import com.example.huetaste.domain.model.DishCategory
import com.example.huetaste.domain.model.DishDetail
import com.example.huetaste.domain.model.DishSearchItem
import com.example.huetaste.domain.model.DishSearchQuery
import com.example.huetaste.domain.model.SearchPage
import com.example.huetaste.domain.repository.DishRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DishRepositoryImpl @Inject constructor(
    private val api: HueTasteApi,
    private val gson: Gson,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DishRepository {

    override suspend fun getDishDetail(dishId: String): DishDetail = withContext(ioDispatcher) {
        apiCall(gson) { api.getDishDetail(dishId).toDomain() }
    }

    override suspend fun getCategories(): List<DishCategory> = withContext(ioDispatcher) {
        apiCall(gson) { api.getCategories().map { it.toDomain() } }
    }

    override suspend fun getDishes(size: Int): List<Dish> = withContext(ioDispatcher) {
        apiCall(gson) { api.getDishes(page = 1, size = size).items.orEmpty().map { it.toDomain() } }
    }

    override suspend fun searchDishes(query: DishSearchQuery, page: Int): SearchPage<DishSearchItem> =
        withContext(ioDispatcher) {
            apiCall(gson) {
                val result = api.searchDishes(
                    query = query.text.trim().ifEmpty { null },
                    categoryId = query.categoryId,
                    ingredients = query.ingredients,
                    sort = query.sort.code,
                    page = page,
                    size = SearchPage.PAGE_SIZE,
                )
                val items = result.items.orEmpty().map { it.toSearchItem() }
                SearchPage(items = items, total = result.total ?: items.size, page = page)
            }
        }

    override suspend fun getFavoriteDishIds(): Set<String> = withContext(ioDispatcher) {
        apiCall(gson) { api.getFavorites(FAVORITE_TYPE_DISH).monAn.orEmpty().map { it.idMonAn }.toSet() }
    }

    override suspend fun setFavorite(dishId: String, favorite: Boolean) {
        withContext(ioDispatcher) {
            apiCall(gson) {
                if (favorite) api.addFavoriteDish(YeuThichMonAnBody(dishId)) else api.removeFavoriteDish(dishId)
            }
        }
    }

    private companion object {
        const val FAVORITE_TYPE_DISH = "mon_an"
    }
}
