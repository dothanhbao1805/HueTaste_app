package com.base.common.widget

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.base.R
import com.base.common.extension.animateBackgroundTintColorChange
import com.base.common.extension.animateTintColorChange
import com.base.common.util.theme.ColorManager
import com.base.common.util.theme.UIChangedListener
import com.base.common.util.theme.UIManager
import com.base.common.util.theme.ViewStyler
import com.google.android.material.imageview.ShapeableImageView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import com.base.common.extension.isNightMode
import com.base.data.Prefs
import io.reactivex.subjects.Subject

@AndroidEntryPoint
class LsImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
): ShapeableImageView(context, attrs), UIChangedListener {

    @Inject lateinit var colorManager: ColorManager
    @Inject lateinit var prefs: Prefs

    private val gestureHelper by lazy { LsGestureHelper(this) }
    private var enableGesture = true
    private var valueAnimator: ValueAnimator? = null
    private var colorTintAttr = ColorManager.COLOR_NONE
    private var rippleBackgroundAttr = ColorManager.RIPPLE_NONE
    private var hadBackground = false
    private var backgroundTintAttr = ColorManager.COLOR_NONE

    init {
        // ShapeableImageView erases everything outside its shape, background included, and fits that shape inside
        // the view padding. A padded icon on a round background would be left with a square of background, so
        // padding here only insets the image; the shape, and the background it reveals, keep the full view size.
        val left = paddingLeft
        val top = paddingTop
        val right = paddingRight
        val bottom = paddingBottom
        super.setPadding(0, 0, 0, 0)
        setContentPadding(left, top, right, bottom)

        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.background)) {
            hadBackground = getResourceId(0, 0) != 0
        }

        context.withStyledAttributes(attrs, R.styleable.LsImageView) {
            colorTintAttr = getInt(R.styleable.LsImageView_colorTint, ColorManager.COLOR_NONE)
            backgroundTintAttr = getInt(R.styleable.LsImageView_bgColorTint, ColorManager.COLOR_NONE)
            enableGesture = getBoolean(R.styleable.LsImageView_enableGesture, false)
            rippleBackgroundAttr = getInt(R.styleable.LsImageView_rippleBackground, -1)
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

    /**
     * Always content padding (see init). ShapeableImageView also calls this on its first measure with the total
     * padding, which is the content padding here, so that call changes nothing instead of doubling it.
     */
    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        setContentPadding(left, top, right, bottom)
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        setContentPaddingRelative(start, top, end, bottom)
    }

    fun setClicks(clicks: (Pair<Float, Float>) -> Unit) {
        gestureHelper.clicks = clicks
    }

    fun setLongClicks(longClicks: (Pair<Float, Float>) -> Unit) {
        gestureHelper.longClicks = longClicks
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

        colorManager.theme().colorById(colorTintAttr)?.let { color ->
            if (animate) {
                valueAnimator = animateTintColorChange(endColor = color)
            } else {
                imageTintList = ColorStateList.valueOf(color)
            }
        }
        colorManager.theme().colorById(backgroundTintAttr)?.let { color ->
            if (animate) {
                valueAnimator = animateBackgroundTintColorChange(endColor = color)
            } else {
                backgroundTintList = ColorStateList.valueOf(color)
            }
        }
    }

    override fun onThemeChanged(withAnim: Boolean) {
        updateUi(withAnim)
    }

}