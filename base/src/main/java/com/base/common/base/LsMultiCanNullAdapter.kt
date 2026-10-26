package com.base.common.base

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject

abstract class LsMultiCanNullAdapter<T, VB: ViewBinding>(private val isNotifyDataChanged: Boolean = true) : RecyclerView.Adapter<LsViewHolder<VB>>() {

    val subjectDataChanged: Subject<List<T>> = BehaviorSubject.createDefault(listOf())

    var data: List<T> = ArrayList()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            if (isNotifyDataChanged) notifyDataSetChanged()
            subjectDataChanged.onNext(value)
        }

    abstract fun bindItem(item: T, binding: VB, position: Int)

    override fun onBindViewHolder(holder: LsViewHolder<VB>, position: Int) {
        val item = getItem(position)
        bindItem(item, holder.binding, position)
    }

    fun getItem(position: Int): T {
        return data[position]
    }

    override fun getItemCount(): Int {
        return data.size
    }

}