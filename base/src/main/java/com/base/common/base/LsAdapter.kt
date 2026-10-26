package com.base.common.base

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

abstract class LsAdapter<T, VB: ViewBinding>(
    val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    private val isNotifyDataChanged: Boolean = true
) : RecyclerView.Adapter<LsViewHolder<VB>>() {

    var data: List<T> = listOf()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            if (isNotifyDataChanged) notifyDataSetChanged()
        }

    val selection = mutableListOf<Long>()
    val selectionChanges: Subject<List<Long>> = BehaviorSubject.create()

    protected fun toggleSelection(id: Long): Boolean {
        when (selection.contains(id)) {
            true -> selection.remove(id)
            false -> selection.add(id)
        }

        selectionChanges.onNext(selection)
        return selection.contains(id)
    }

    protected fun isSelected(id: Long): Boolean {
        return selection.contains(id)
    }

    fun clearSelection() {
        selection.clear()
        selectionChanges.onNext(selection)
        notifyDataSetChanged()
    }

    abstract fun bindItem(item: T, binding: VB, position: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LsViewHolder<VB> {
        return LsViewHolder(bindingInflater(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: LsViewHolder<VB>, position: Int) {
        val item = getItem(position) ?: return
        bindItem(item, holder.binding, position)
    }

    open fun getItem(position: Int): T? {
        return data.getOrNull(position)
    }

    override fun getItemCount(): Int {
        return data.size
    }

}