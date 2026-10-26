package com.base.common.base

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class PaginatedListAdapter<T, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val pageSize: Int = 100
) : ListAdapter<T, VH>(diffCallback) {

    private var fullList: List<T> = emptyList()
    private var currentLimit = pageSize

    override fun getItemCount(): Int = minOf(currentLimit, fullList.size)

    override fun getItem(position: Int): T = fullList[position]

    fun submitFullList(newList: List<T>) {
        fullList = newList
        currentLimit = pageSize
        submitList(fullList.take(currentLimit))
    }

    fun submitFullList(newList: List<T>, callback: Runnable) {
        fullList = newList
        currentLimit = pageSize
        submitList(fullList.take(currentLimit), callback)
    }

    fun loadMore() {
        if (currentLimit < fullList.size) {
            currentLimit = (currentLimit + pageSize).coerceAtMost(fullList.size)
            submitList(fullList.take(currentLimit))
        }
    }

    fun reset() {
        currentLimit = pageSize
        submitList(fullList.take(currentLimit))
    }
}
