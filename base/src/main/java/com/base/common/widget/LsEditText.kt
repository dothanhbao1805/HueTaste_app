package com.base.common.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.base.R
import com.base.common.extension.animateTextHintColorChange
import com.base.common.extension.animateTextColorChange
import com.base.common.extension.animateTextLinkColorChange
import com.base.common.util.theme.ColorManager
import com.base.common.util.theme.FontManager
import com.base.common.util.theme.SizeManager
import com.base.common.util.theme.UIChangedListener
import com.base.common.util.theme.UIManager
import com.base.common.util.theme.ViewStyler
import com.base.data.Prefs
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import com.base.common.extension.isNightMode

@AndroidEntryPoint
class LsEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs), UIChangedListener {

    @Inject lateinit var fontManager: FontManager
    @Inject lateinit var colorManager: ColorManager

    private var valueAnimator: ValueAnimator? = null
    private var textColorAttr = ColorManager.COLOR_NONE
    private var textHintColorAttr = ColorManager.COLOR_NONE
    private var textLinkColorAttr = ColorManager.COLOR_NONE
    private var textFontAttr = FontManager.FONT_REGULAR
    private var textSizeAttr = -1
    private var hadFont = false

    var onSelectionChangedListener: ((selStart: Int, selEnd: Int) -> Unit)? = null

    init {
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.fontFamily)) {
            hadFont = getResourceId(0, 0) != 0
        }

        context.withStyledAttributes(attrs, R.styleable.LsEditText) {
            textColorAttr = getInt(R.styleable.LsEditText_textColor, ColorManager.COLOR_NONE)
            textHintColorAttr = getInt(R.styleable.LsEditText_textHintColor, ColorManager.COLOR_NONE)
            textLinkColorAttr = getInt(R.styleable.LsEditText_textLinkColor, ColorManager.COLOR_NONE)
            textFontAttr = getInt(R.styleable.LsEditText_textFont, FontManager.FONT_REGULAR)
            textSizeAttr = getInt(R.styleable.LsEditText_textSize, -1)
        }

        when {
            isInEditMode ->  ViewStyler.applyAttributesView(this, attrs, textSizeAttr, hadFont, context.isNightMode())
            else -> {
                if (!hadFont) {
                    fontManager.get(textFontAttr) { typeface ->
                        setTypeface(typeface, this.typeface?.style ?: Typeface.NORMAL)
                    }
                }

                if (textSizeAttr != -1){
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, SizeManager.sizeById(context, false, Prefs.TEXT_SIZE_NORMAL, textSizeAttr))
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textCursorDrawable = ContextCompat.getDrawable(context, R.drawable.cursor)?.apply {
                        setTint(colorManager.theme().textPrimary)
                    }
                }

                updateUi()
            }
        }
    }

     override fun onAttachedToWindow() {
        UIManager.addListener(this)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        if (valueAnimator != null) {
            valueAnimator!!.cancel()
        }
        UIManager.removeListener(this)
        super.onDetachedFromWindow()
    }

    fun updateUi(animate: Boolean = false) {
        colorManager.theme().colorById(textColorAttr)?.let { color ->
            if (animate) {
                valueAnimator = animateTextColorChange(endColor = color)
            } else {
                setTextColor(ColorStateList.valueOf(color))
            }
        }
        colorManager.theme().colorById(textHintColorAttr)?.let { color ->
            if (animate) {
                valueAnimator = animateTextHintColorChange(endColor = color)
            } else {
                setHintTextColor(ColorStateList.valueOf(color))
            }
        }
        colorManager.theme().colorById(textLinkColorAttr)?.let { color ->
            if (animate) {
                valueAnimator = animateTextLinkColorChange(endColor = color)
            } else {
                setHintTextColor(ColorStateList.valueOf(color))
            }
        }
    }

    override fun onThemeChanged(withAnim: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val drawable = ContextCompat.getDrawable(context, R.drawable.cursor)?.apply { setTint(colorManager.theme().textPrimary) }
            textCursorDrawable = drawable
        }

        updateUi(withAnim)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        onSelectionChangedListener?.invoke(selStart, selEnd)
    }

    override fun onLanguageChanged(withAnim: Boolean?) {

    }

}