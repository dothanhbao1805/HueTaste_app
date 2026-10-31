package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.DishDetail

interface DishRepository {
    suspend fun getDishDetail(dishId: String): DishDetail
}
