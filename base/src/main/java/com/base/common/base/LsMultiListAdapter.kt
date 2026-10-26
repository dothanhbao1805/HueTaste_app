package com.base.common.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.viewbinding.ViewBinding
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

abstract class LsMultiListAdapter<T : Any, VB : ViewBinding>(
    private val diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, LsViewHolder<VB>>(diffCallback) {

    val selection = mutableListOf<Long>()
    val selectionChanges: Subject<List<Long>> = BehaviorSubject.create()

    protected fun toggleSelection(id: Long): Boolean {
        if (selection.contains(id)) {
            selection.remove(id)
        } else {
            selection.add(id)
        }
        selectionChanges.onNext(selection.toList())
        return selection.contains(id)
    }

    protected fun isSelected(id: Long): Boolean {
        return selection.contains(id)
    }

    open fun clearSelection() {
        selection.clear()
        selectionChanges.onNext(selection.toList())
        notifyDataSetChanged()
    }

    abstract fun bindItem(item: T, binding: VB, position: Int)

    override fun onBindViewHolder(holder: LsViewHolder<VB>, position: Int) {
        val item = getItem(position) ?: return
        bindItem(item, holder.binding, position)
    }
}
