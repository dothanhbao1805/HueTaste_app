package com.example.huetaste.data.repository

import com.example.huetaste.data.local.RecentSearchStore
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.repository.RecentSearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RecentSearchRepositoryImpl @Inject constructor(
    private val store: RecentSearchStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RecentSearchRepository {
    override suspend fun get(): List<String> = withContext(ioDispatcher) { store.get() }

    override suspend fun add(term: String) = withContext(ioDispatcher) { store.add(term) }

    override suspend fun remove(term: String) = withContext(ioDispatcher) { store.remove(term) }

    override suspend fun clear() = withContext(ioDispatcher) { store.clear() }
}
