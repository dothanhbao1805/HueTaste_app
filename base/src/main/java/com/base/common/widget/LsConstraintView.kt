package com.base.common.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.base.R
import com.base.common.extension.animBackgroundColorChange
import com.base.common.util.theme.ColorManager
import com.base.common.util.theme.UIChangedListener
import com.base.common.util.theme.UIManager
import com.base.common.util.theme.ViewStyler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import com.base.common.extension.isNightMode
import com.base.data.Prefs

@AndroidEntryPoint
class LsConstraintView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet
) : ConstraintLayout(context, attrs),
    UIChangedListener {

    @Inject lateinit var colorManager: ColorManager
    @Inject lateinit var prefs: Prefs

    private val gestureHelper by lazy { LsGestureHelper(this) }
    private var enableGesture = true
    private var valueAnimator: ValueAnimator? = null
    private var backgroundTintAttr = ColorManager.COLOR_NONE
    private var rippleBackgroundAttr = ColorManager.RIPPLE_NONE
    private var hadBackground = false

    init {
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.background)) {
            hadBackground = getResourceId(0, 0) != 0
        }

        context.withStyledAttributes(attrs, R.styleable.LsConstraintView) {
            backgroundTintAttr = getInt(R.styleable.LsConstraintView_bgColorTint, ColorManager.COLOR_NONE)
            rippleBackgroundAttr = getInt(R.styleable.LsConstraintView_rippleBackground, -1)
            enableGesture = getBoolean(R.styleable.LsConstraintView_enableGesture, false)
        }

        if (!hadBackground && backgroundTintAttr != ColorManager.COLOR_NONE){
            setBackgroundColor(Color.WHITE)
        }

        when {
            isInEditMode -> ViewStyler.applyAttributesView(this, attrs, hadBackground, context.isNightMode())
            else -> {
                if (enableGesture) {
                    gestureHelper.attachTo()
                    gestureHelper.enableGesture = enableGesture
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

    private fun updateUi(animate: Boolean = false) {
        when {
            hadBackground -> { }
            rippleBackgroundAttr == ColorManager.RIPPLE && prefs.isNightMode() -> {
                setBackgroundResource(R.drawable.ripple_dark)
            }
            rippleBackgroundAttr == ColorManager.RIPPLE && !prefs.isNightMode() -> {
                setBackgroundResource(R.drawable.ripple_light)
            }
            rippleBackgroundAttr == ColorManager.RIPPLE_BORDERLESS && prefs.isNightMode() -> {
                setBackgroundResource(R.drawable.ripple_borderless_dark)
            }
            rippleBackgroundAttr == ColorManager.RIPPLE_BORDERLESS && !prefs.isNightMode() -> {
                setBackgroundResource(R.drawable.ripple_borderless_light)
            }
        }

        colorManager.theme().colorById(backgroundTintAttr)?.let { color ->
            if (animate) {
                valueAnimator = animBackgroundColorChange(endColor = color)
            } else {
                backgroundTintList = ColorStateList.valueOf(color)
            }
        }
    }

    override fun onThemeChanged(withAnim: Boolean) {
        updateUi(withAnim)
    }

}