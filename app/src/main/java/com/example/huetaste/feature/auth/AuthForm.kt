package com.example.huetaste.feature.auth

import android.graphics.Rect
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import com.base.common.extension.clicks
import com.base.common.widget.LsEditText
import com.example.huetaste.R

/** Outlines the pill around [this] input in the focus color while it has focus. */
internal fun LsEditText.trackFocus(container: View) {
    onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus -> container.isActivated = hasFocus }
}

/** Shows [message] under a field and outlines the field in red, or clears both when [message] is null. */
internal fun showFieldError(container: View, row: View, text: TextView, @StringRes message: Int?) {
    container.isSelected = message != null
    row.isVisible = message != null
    if (message != null) text.setText(message)
}

/** Lets [toggle] switch this password input between hidden and visible text, keeping the cursor where it was. */
internal fun LsEditText.bindPasswordToggle(toggle: ImageView) {
    var visible = false
    toggle.clicks(withAnim = false) {
        visible = !visible
        val cursor = selectionEnd
        transformationMethod = if (visible) null else PasswordTransformationMethod.getInstance()
        setSelection(cursor.coerceIn(0, text?.length ?: 0))
        toggle.setImageResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
        toggle.contentDescription = context.getString(if (visible) R.string.auth_hide_password else R.string.auth_show_password)
    }
}

/** Scrolls so the focused field stays visible above the keyboard. */
internal fun NestedScrollView.scrollToFocused(focused: View?) {
    val content = getChildAt(0) ?: return
    val field = focused ?: return
    post {
        val rect = Rect()
        field.getDrawingRect(rect)
        offsetDescendantRectToMyCoords(field, rect)
        requestChildRectangleOnScreen(content, rect, false)
    }
}
