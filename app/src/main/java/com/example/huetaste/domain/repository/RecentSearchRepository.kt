package com.example.huetaste.domain.repository

interface RecentSearchRepository {
    /** Newest first. */
    suspend fun get(): List<String>

    suspend fun add(term: String)

    suspend fun remove(term: String)

    suspend fun clear()
}
