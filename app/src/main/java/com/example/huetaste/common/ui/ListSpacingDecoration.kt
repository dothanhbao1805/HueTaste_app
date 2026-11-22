package com.example.huetaste.common.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Puts [gap] between the cards of a list or grid without an outer margin, so the cards line up with the rest of
 * the screen's content edges. The first row gets no top gap either, because the list already has top padding.
 */
class ListSpacingDecoration(
    private val spanCount: Int,
    private val gap: Int,
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return
        val half = gap / 2
        if (spanCount > 1) {
            val column = (parent.layoutManager as? GridLayoutManager)?.spanSizeLookup?.getSpanIndex(position, spanCount)
                ?: (position % spanCount)
            outRect.left = if (column == 0) 0 else half
            outRect.right = if (column == spanCount - 1) 0 else half
        }
        outRect.top = if (position < spanCount) 0 else gap
    }
}
