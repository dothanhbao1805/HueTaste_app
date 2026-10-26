package com.base.common.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class LsPageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    val fragments = arrayListOf<Fragment>()

    fun addFragments(vararg fragments: Fragment){
        this.fragments.addAll(fragments)
    }

    override fun getItemCount() = fragments.size

    override fun createFragment(position: Int) = fragments[position]

    // Thêm phương thức để xóa fragment theo vị trí
    fun removeFragment(position: Int) {
        if (position < fragments.size) {
            fragments.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, fragments.size)
        }
    }

}