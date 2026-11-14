package com.example.huetaste.domain.repository

import com.example.huetaste.domain.model.Favorites

interface FavoriteRepository {
    /** Every saved dish and place of the signed-in user in one request. */
    suspend fun getFavorites(): Favorites
}
