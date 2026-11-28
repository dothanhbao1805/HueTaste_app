package com.example.huetaste.common.extension

import android.graphics.Rect
import android.view.View
import android.view.ViewParent
import androidx.core.view.doOnNextLayout
import androidx.core.widget.NestedScrollView

private const val FIELD_MARGIN_DP = 16

/**
 * Scrolls so [focused] ends above the keyboard. The keyboard is left for as bottom padding of this view; the framework
 * does not count that padding when it brings a child on screen, so a field behind the keyboard would count as visible.
 */
fun NestedScrollView.scrollToFocused(focused: View?) {
    val field = focused ?: return
    post {
        // The padding was just changed: measure against the layout that has it.
        if (isLayoutRequested) doOnNextLayout { scrollFieldAboveKeyboard(field) } else scrollFieldAboveKeyboard(field)
    }
}

private fun NestedScrollView.scrollFieldAboveKeyboard(field: View) {
    if (!field.isAttachedToWindow) return
    val rect = Rect()
    field.getDrawingRect(rect)
    offsetDescendantRectToMyCoords(field, rect)
    val margin = (FIELD_MARGIN_DP * resources.displayMetrics.density).toInt()
    val visibleBottom = scrollY + height - paddingBottom
    when {
        rect.bottom + margin > visibleBottom -> smoothScrollBy(0, rect.bottom + margin - visibleBottom)
        rect.top - margin < scrollY -> smoothScrollBy(0, rect.top - margin - scrollY)
    }
}

/** For a screen that hosts the scrolling page (a pager): finds the focused field below this view and scrolls its page. */
fun View.scrollFocusedIntoView() {
    findFocus()?.scrollIntoViewAboveKeyboard(onlyWithKeyboard = false)
}

/**
 * Moving focus between fields does not change the keyboard, so nothing else would scroll the next field into view.
 * With [onlyWithKeyboard] this does nothing until the keyboard has made room (the scroll has bottom padding).
 */
fun View.scrollIntoViewAboveKeyboard(onlyWithKeyboard: Boolean = true) {
    var ancestor: ViewParent? = this.parent
    while (ancestor != null && ancestor !is NestedScrollView) ancestor = ancestor.parent
    val scroll = ancestor as? NestedScrollView ?: return
    if (!onlyWithKeyboard || scroll.paddingBottom > 0) scroll.scrollToFocused(this)
}
