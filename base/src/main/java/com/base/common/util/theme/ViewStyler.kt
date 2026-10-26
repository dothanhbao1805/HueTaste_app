package com.base.common.util.theme

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.base.common.extension.getColorCompat
import com.base.common.extension.setTint
import com.base.common.widget.LsCardView
import com.base.common.widget.LsConstraintView
import com.base.R
import com.base.common.widget.LsEditText
import com.base.common.widget.LsFrameView
import com.base.common.widget.LsImageView
import com.base.common.widget.LsLinearView
import com.base.common.widget.LsRelativeView
import com.base.common.widget.LsTextView
import com.base.common.widget.LsView
import com.base.data.Prefs
import androidx.core.content.withStyledAttributes
import com.base.common.extension.isNightMode
import com.base.common.widget.LsPreferenceView

object ViewStyler {

    fun applyAttributesView(view: LsTextView, attrs: AttributeSet?, textSizeDefaultAttr: Int = -1, hadFont: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsTextView) {
            val textColorAttr = getInt(R.styleable.LsTextView_textColor, ColorManager.COLOR_NONE)
            val textSizeAttr = getInt(R.styleable.LsTextView_textSize, Prefs.TEXT_SIZE_NORMAL)
            val textFontAttr = getInt(R.styleable.LsTextView_textFont, FontManager.FONT_REGULAR)

            val textColor = ColorManager.colorById(view.context, textColorAttr, isNightMode)
            val textFont = FontManager.fontById(view.context, textFontAttr)
            val textSize = SizeManager.sizeById(view.context, view.isInEditMode, Prefs.TEXT_SIZE_NORMAL, textSizeAttr)

            view.run {
                textColor?.let { color -> setTextColor(color) }
                if (!hadFont) {
                    textFont?.let { typeface ->
                        setTypeface(typeface, view.typeface?.style ?: Typeface.NORMAL)
                    }
                }
                if (textSizeDefaultAttr != -1) {
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                }
            }

        }
    }

    fun applyAttributesView(view: LsEditText, attrs: AttributeSet?, textSizeDefaultAttr: Int = -1, hadFont: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsEditText) {
            val textColorAttr = getInt(R.styleable.LsEditText_textColor, ColorManager.COLOR_NONE)
            val textHintColorAttr = getInt(R.styleable.LsEditText_textHintColor, ColorManager.COLOR_NONE)
            val textSizeAttr = getInt(R.styleable.LsEditText_textSize, Prefs.TEXT_SIZE_NORMAL)
            val textFontAttr = getInt(R.styleable.LsEditText_textFont, FontManager.FONT_REGULAR)

            val textColor = ColorManager.colorById(view.context, textColorAttr, isNightMode)
            val textHintColor = ColorManager.colorById(view.context, textHintColorAttr, isNightMode)
            val textFont = FontManager.fontById(view.context, textFontAttr)
            val textSize = SizeManager.sizeById(view.context, view.isInEditMode, Prefs.TEXT_SIZE_NORMAL, textSizeAttr)

            view.run {
                textColor?.let { color -> setTextColor(color) }
                textHintColor?.let { color -> setHintTextColor(color) }
                if (!hadFont) {
                    textFont?.let { typeface ->
                        setTypeface(typeface, view.typeface?.style ?: Typeface.NORMAL)
                    }
                }
                if (textSizeDefaultAttr != -1) {
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                }
                val drawable = ContextCompat.getDrawable(context, R.drawable.cursor)?.apply { setTint(context.getColorCompat(R.color.textPrimaryDark)) }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    textCursorDrawable = drawable
                }
            }
        }
    }

    fun applyAttributesView(view: LsImageView, attrs: AttributeSet?, hadBackground: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsImageView) {
            val colorTintAttr = getInt(R.styleable.LsImageView_colorTint, ColorManager.COLOR_NONE)
            val bgColorTintAttr = getInt(R.styleable.LsImageView_bgColorTint, ColorManager.COLOR_NONE)
            val colorTint = ColorManager.colorById(view.context, colorTintAttr, isNightMode)
            val bgColorTint = ColorManager.colorById(view.context, bgColorTintAttr, isNightMode)

            view.run {
                colorTint?.let { color -> setTint(color) }
                bgColorTint?.let { color ->
                    when {
                        hadBackground -> backgroundTintList = ColorStateList.valueOf(color)
                        else -> setBackgroundColor(color)
                    }
                }
            }
        }
    }

    fun applyAttributesView(view: LsCardView, attrs: AttributeSet?, hadCustomShadow: Boolean, cardElevation: Float, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsCardView) {
            val bgColorAttr = getInt(R.styleable.LsCardView_bgColor, ColorManager.COLOR_NONE)
            val strokeColorAttr = getInt(R.styleable.LsCardView_strColor, ColorManager.COLOR_NONE)
            val bgColor = ColorManager.colorById(view.context, bgColorAttr, isNightMode)
            val strokeColor = ColorManager.colorById(view.context, strokeColorAttr, isNightMode)

            view.run {
                bgColor?.let { color -> setCardBackgroundColor(color) }
                strokeColor?.let { color -> setStrokeColor(color) }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                when {
                    hadCustomShadow -> { }
                    cardElevation > 0f && view.context.isNightMode() -> {
                        view.outlineAmbientShadowColor = ContextCompat.getColor(view.context, R.color.shadowDark)
                        view.outlineSpotShadowColor = ContextCompat.getColor(view.context, R.color.shadowDark)
                    }
                    cardElevation > 0f -> {
                        view.outlineAmbientShadowColor = ContextCompat.getColor(view.context, R.color.shadowLight)
                        view.outlineSpotShadowColor = ContextCompat.getColor(view.context, R.color.shadowLight)
                    }
                }
            }
        }
    }

    fun applyAttributesView(view: LsConstraintView, attrs: AttributeSet?, hadBackground: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsConstraintView) {
            val bgColorTintAttr = getInt(R.styleable.LsConstraintView_bgColorTint, ColorManager.COLOR_NONE)
            val bgColorTint = ColorManager.colorById(view.context, bgColorTintAttr, isNightMode)

            view.run {
                bgColorTint?.let { color ->
                    when {
                        hadBackground -> backgroundTintList = ColorStateList.valueOf(color)
                        else -> setBackgroundColor(color)
                    }
                }
            }
        }
    }

    fun applyAttributesView(view: LsLinearView, attrs: AttributeSet?, hadBackground: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsLinearView) {
            val bgColorTintAttr = getInt(R.styleable.LsLinearView_bgColorTint, ColorManager.COLOR_NONE)
            val bgColorTint = ColorManager.colorById(view.context, bgColorTintAttr, isNightMode)

            view.run {
                bgColorTint?.let { color ->
                    when {
                        hadBackground -> backgroundTintList = ColorStateList.valueOf(color)
                        else -> setBackgroundColor(color)
                    }
                }
            }
        }
    }

    fun applyAttributesView(view: LsPreferenceView, viewClicks: LsCardView, attrs: AttributeSet?, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsPreferenceView) {
            val bgColorTintAttr = getInt(R.styleable.LsPreferenceView_bgColorTint, ColorManager.COLOR_NONE)
            val color = ColorManager.colorById(view.context, bgColorTintAttr, isNightMode)

            view.run {
                when {
                    color == null -> viewClicks.setCardBackgroundColor(Color.TRANSPARENT)
                    else -> viewClicks.setCardBackgroundColor(ColorStateList.valueOf(color))
                }
            }
        }
    }

    fun applyAttributesView(view: LsRelativeView, attrs: AttributeSet?, hadBackground: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsRelativeView) {
            val bgColorTintAttr = getInt(R.styleable.LsRelativeView_bgColorTint, ColorManager.COLOR_NONE)
            val bgColorTint = ColorManager.colorById(view.context, bgColorTintAttr, isNightMode)

            view.run {
                bgColorTint?.let { color ->
                    when {
                        hadBackground -> backgroundTintList = ColorStateList.valueOf(color)
                        else -> setBackgroundColor(color)
                    }
                }
            }
        }
    }

    fun applyAttributesView(view: LsFrameView, attrs: AttributeSet?, hadBackground: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsFrameView) {
            val bgColorTintAttr = getInt(R.styleable.LsFrameView_bgColorTint, ColorManager.COLOR_NONE)
            val bgColorTint = ColorManager.colorById(view.context, bgColorTintAttr, isNightMode)

            view.run {
                bgColorTint?.let { color ->
                    when {
                        hadBackground -> backgroundTintList = ColorStateList.valueOf(color)
                        else -> setBackgroundColor(color)
                    }
                }
            }
        }
    }

    fun applyAttributesView(view: LsView, attrs: AttributeSet?, hadBackground: Boolean, isNightMode: Boolean) {
        view.context.withStyledAttributes(attrs, R.styleable.LsView) {
            val bgColorTintAttr = getInt(R.styleable.LsView_bgColorTint, ColorManager.COLOR_NONE)
            val bgColorTint = ColorManager.colorById(view.context, bgColorTintAttr, isNightMode)

            view.run {
                bgColorTint?.let { color ->
                    when {
                        hadBackground -> backgroundTintList = ColorStateList.valueOf(color)
                        else -> setBackgroundColor(color)
                    }
                }
            }
        }
    }

}