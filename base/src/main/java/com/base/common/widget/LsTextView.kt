package com.base.common.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import com.base.R
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
import timber.log.Timber
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import com.base.common.extension.isNightMode

@AndroidEntryPoint
class LsTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs), UIChangedListener {

    @Inject lateinit var fontManager: FontManager
    @Inject lateinit var colorManager: ColorManager

    // For language
    private var textResourceId: Int = -1

    // For theme
    private var valueAnimator: ValueAnimator? = null
    private var textColorAttr = ColorManager.COLOR_NONE
    private var textLinkColorAttr = ColorManager.COLOR_NONE
    private var textFontAttr = FontManager.FONT_REGULAR
    private var textSizeAttr = -1
    private var hadFont = false

    init {
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.fontFamily)) {
            hadFont = getResourceId(0, 0) != 0
        }

        context.withStyledAttributes(attrs, R.styleable.LsTextView) {
            textColorAttr = getInt(R.styleable.LsTextView_textColor, ColorManager.COLOR_NONE)
            textLinkColorAttr = getInt(R.styleable.LsTextView_textLinkColor, ColorManager.COLOR_NONE)
            textFontAttr = getInt(R.styleable.LsTextView_textFont, FontManager.FONT_REGULAR)
            textSizeAttr = getInt(R.styleable.LsTextView_textSize, -1)
        }

        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.text)) {
            textResourceId = getResourceId(0, -1)
        }

        when {
            isInEditMode -> ViewStyler.applyAttributesView(this, attrs, textSizeAttr, hadFont, context.isNightMode())
            else -> {
                if (!hadFont) {
                    fontManager.get(textFontAttr) { typeface ->
                        setTypeface(typeface, this.typeface?.style ?: Typeface.NORMAL)
                    }
                }

                if (textSizeAttr != -1){
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, SizeManager.sizeById(context, false, Prefs.TEXT_SIZE_NORMAL, textSizeAttr))
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
        colorManager.theme().colorById(textLinkColorAttr)?.let { color ->
            if (animate) {
                valueAnimator = animateTextLinkColorChange(endColor = color)
            } else {
                setLinkTextColor(ColorStateList.valueOf(color))
            }
        }
    }

    fun setTextSize(textSizeAttr: Int) {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, SizeManager.sizeById(context, false, Prefs.TEXT_SIZE_NORMAL, textSizeAttr))
    }

    fun setTextFont(fontAttr: Int) {
        fontManager.get(fontAttr) { typeface ->
            setTypeface(typeface, this.typeface?.style ?: Typeface.NORMAL)
        }
    }

    override fun onThemeChanged(withAnim: Boolean) {
        updateUi(withAnim)
    }

    override fun onLanguageChanged(withAnim: Boolean?) {
        textResourceId.takeIf { it != -1 }?.let { textResourceId ->
            text = context.getString(textResourceId)
        }
    }

}