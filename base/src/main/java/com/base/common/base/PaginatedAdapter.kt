package com.base.common.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding

abstract class PaginatedAdapter<T, VB: ViewBinding>(
    bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    isNotifyDataChanged: Boolean = true,
    private val pageSize: Int = 100
) : LsAdapter<T, VB>(bindingInflater, isNotifyDataChanged) {

    private var fullList: List<T> = emptyList()
    private var currentLimit = pageSize

    override fun getItemCount(): Int = minOf(currentLimit, fullList.size)

    override fun getItem(position: Int): T? = fullList.getOrNull(position)

    /**
     * Nạp dữ liệu mới, mặc định chỉ lấy pageSize item đầu
     */
    fun submitFullList(newList: List<T>) {
        fullList = newList
        currentLimit = pageSize
        data = fullList.take(currentLimit)
    }

    fun submitFullList(newList: List<T>, callback: Runnable) {
        fullList = newList
        currentLimit = pageSize
        data = fullList.take(currentLimit)
        callback.run()
    }

    fun loadMore() {
        if (currentLimit < fullList.size) {
            currentLimit = (currentLimit + pageSize).coerceAtMost(fullList.size)
            data = fullList.take(currentLimit)
        }
    }

    fun reset() {
        currentLimit = pageSize
        data = fullList.take(currentLimit)
    }
}