package com.example.huetaste.feature.auth

import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.base.common.extension.clicks
import com.base.common.widget.LsEditText
import com.example.huetaste.R
import com.example.huetaste.common.extension.scrollIntoViewAboveKeyboard
import com.example.huetaste.domain.model.AuthValidator
import com.example.huetaste.domain.model.PasswordStrength

/** Outlines the pill around [this] input in the focus color while it has focus. */
internal fun LsEditText.trackFocus(container: View) {
    onFocusChangeListener = View.OnFocusChangeListener { field, hasFocus ->
        container.isActivated = hasFocus
        if (hasFocus) field.scrollIntoViewAboveKeyboard()
    }
}

/** Shows [message] under a field and outlines the field in red, or clears both when [message] is null. */
internal fun showFieldError(container: View, row: View, text: TextView, @StringRes message: Int?) {
    container.isSelected = message != null
    row.isVisible = message != null
    if (message != null) text.setText(message)
}

/** Fills the three [bars] and the [label] in [block] for [password]; the block hides while the password is empty. */
internal fun showPasswordStrength(block: View, bars: List<View>, label: TextView, password: String) {
    block.isVisible = password.isNotEmpty()
    if (password.isEmpty()) return
    val (level, color, text) = when (AuthValidator.strengthOf(password)) {
        PasswordStrength.WEAK -> Triple(1, R.color.hue_error, R.string.register_strength_weak)
        PasswordStrength.MEDIUM -> Triple(2, R.color.hue_secondary_container, R.string.register_strength_medium)
        PasswordStrength.STRONG -> Triple(3, R.color.hue_primary, R.string.register_strength_strong)
    }
    val context = block.context
    bars.forEachIndexed { index, bar ->
        bar.backgroundTintList = ContextCompat.getColorStateList(context, if (index < level) color else R.color.hue_surface_container_high)
    }
    label.text = context.getString(R.string.register_strength, context.getString(text))
}

/** Lets [toggle] switch this password input between hidden and visible text, keeping the cursor where it was. */
internal fun LsEditText.bindPasswordToggle(toggle: ImageView) {
    var visible = false
    toggle.clicks {
        visible = !visible
        val cursor = selectionEnd
        transformationMethod = if (visible) null else PasswordTransformationMethod.getInstance()
        setSelection(cursor.coerceIn(0, text?.length ?: 0))
        toggle.setImageResource(if (visible) R.drawable.ic_visibility_off else R.drawable.ic_visibility)
        toggle.contentDescription = context.getString(if (visible) R.string.auth_hide_password else R.string.auth_show_password)
    }
}
