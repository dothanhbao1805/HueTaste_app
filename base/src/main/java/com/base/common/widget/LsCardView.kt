package com.base.common.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.base.R
import com.base.common.extension.animCardBackgroundColorChange
import com.base.common.extension.animCardStrokeColorChange
import com.base.common.util.theme.ColorManager
import com.base.common.util.theme.UIChangedListener
import com.base.common.util.theme.UIManager
import com.base.common.util.theme.ViewStyler
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import com.base.common.extension.isNightMode
import com.base.data.Prefs
import timber.log.Timber

@SuppressLint("ResourceType")
@AndroidEntryPoint
class LsCardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : MaterialCardView(context, attrs), UIChangedListener {

    @Inject lateinit var colorManager: ColorManager
    @Inject lateinit var prefs: Prefs

    private val gestureHelper by lazy { LsGestureHelper(this) }
    private var enableGesture = true
    private var valueAnimator: ValueAnimator? = null
    private var backgroundColorAttr = ColorManager.COLOR_NONE
    private var strokeColorAttr = ColorManager.COLOR_NONE
    private var ripColorAttr = ColorManager.COLOR_NONE
    private var hadCustomShadow = false
    private var cardElevation = 0f

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.withStyledAttributes(attrs, intArrayOf(android.R.attr.outlineSpotShadowColor, android.R.attr.outlineAmbientShadowColor)) {
                hadCustomShadow = hasValue(0) || hasValue(1)
            }
        }
        context.withStyledAttributes(attrs, intArrayOf(androidx.cardview.R.attr.cardElevation)) {
            cardElevation = getDimension(0, 0f)
        }

        context.withStyledAttributes(attrs, R.styleable.LsCardView) {
            backgroundColorAttr = getInt(R.styleable.LsCardView_bgColor, ColorManager.COLOR_NONE)
            strokeColorAttr = getInt(R.styleable.LsCardView_strColor, ColorManager.COLOR_NONE)
            ripColorAttr = getInt(R.styleable.LsCardView_ripColor, ColorManager.COLOR_NONE)
            enableGesture = getBoolean(R.styleable.LsCardView_enableGesture, false)
        }

        when {
            isInEditMode -> {
                ViewStyler.applyAttributesView(this, attrs, hadCustomShadow, cardElevation, context.isNightMode())
            }
            else -> {
                if (enableGesture) {
                    gestureHelper.attachTo()
                    gestureHelper.enableGesture = enableGesture
                }

                if (backgroundColorAttr != ColorManager.COLOR_NONE){
                    setCardBackgroundColor(Color.WHITE)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    when {
                        hadCustomShadow -> { }
                        cardElevation > 0f && prefs.isNightMode() -> {
                            outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.shadowDark)
                            outlineSpotShadowColor = ContextCompat.getColor(context, R.color.shadowDark)
                        }
                        cardElevation > 0f -> {
                            outlineAmbientShadowColor = ContextCompat.getColor(context, R.color.shadowLight)
                            outlineSpotShadowColor = ContextCompat.getColor(context, R.color.shadowLight)
                        }
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
        colorManager.theme().colorById(backgroundColorAttr)?.let { color ->
            when {
                animate -> valueAnimator = animCardBackgroundColorChange(endColor = color)
                else -> setCardBackgroundColor(ColorStateList.valueOf(color))
            }
        }
        colorManager.theme().colorById(strokeColorAttr)?.let { color ->
            when {
                animate -> valueAnimator = animCardStrokeColorChange(endColor = color)
                else -> setStrokeColor(ColorStateList.valueOf(color))
            }
        }
        colorManager.theme().colorById(ripColorAttr)?.let { color ->
            rippleColor = ColorStateList.valueOf(color)
        }
    }

    override fun onThemeChanged(withAnim: Boolean) {
        updateUi(withAnim)
    }

}