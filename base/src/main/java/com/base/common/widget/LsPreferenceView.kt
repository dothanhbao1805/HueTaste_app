package com.base.common.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.base.R
import com.base.common.extension.animCardBackgroundColorChange
import com.base.common.extension.setTint
import com.base.common.util.theme.ColorManager
import com.base.common.util.theme.UIChangedListener
import com.base.common.util.theme.UIManager
import com.base.common.util.theme.ViewStyler
import com.base.databinding.PreferenceViewBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import com.base.common.extension.clicks
import com.base.common.extension.isNightMode
import com.base.databinding.WidgetPreferenceSwitchBinding

@SuppressLint("CustomViewStyleable")
@AndroidEntryPoint
class LsPreferenceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs),
    UIChangedListener {

    @Inject lateinit var colorManager: ColorManager

    private var valueAnimator: ValueAnimator? = null
    private var backgroundTintAttr = ColorManager.COLOR_NONE
    private val binding = PreferenceViewBinding.inflate(LayoutInflater.from(context), this, true)
    private var viewWidgetFrame: View? = null

    var primary: Int = -1
        set(value) {
            field = value

            when {
                value != -1 -> binding.primary.text = context.getString(value)
            }
        }

    var secondary: Int = -1
        set(value) {
            field = value

            when {
                value != -1 -> {
                    binding.secondary.text = context.getString(value)
                    binding.secondary.isVisible = context.getString(value).isNotEmpty() == true
                }
                else -> binding.secondary.isVisible = false
            }
        }

    var tertiary: Int = -1
        set(value) {
            field = value

            when {
                value != -1 -> {
                    binding.tertiary.text = context.getString(value)
                    binding.tertiary.isVisible = context.getString(value).isNotEmpty() == true
                }
                else -> binding.tertiary.isVisible = false
            }
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.LsPreferenceView) {
            backgroundTintAttr = getInt(R.styleable.LsPreferenceView_bgColorTint, ColorManager.COLOR_NONE)
        }

        when {
            isInEditMode -> ViewStyler.applyAttributesView(this, binding.viewClicks, attrs, context.isNightMode())
            else -> updateUi(false)
        }

        context.withStyledAttributes(attrs, R.styleable.LsPreferenceView) {
            primary = getResourceId(R.styleable.LsPreferenceView_primary, -1)
            secondary = getResourceId(R.styleable.LsPreferenceView_secondary, -1)
            tertiary = getResourceId(R.styleable.LsPreferenceView_tertiary, -1)

            getResourceId(R.styleable.LsPreferenceView_widget, -1).takeIf { it != -1 }?.let { id ->
                viewWidgetFrame = View.inflate(context, id, binding.widgetFrame)
            }

            getResourceId(R.styleable.LsPreferenceView_preview, -1).takeIf { it != -1 }?.let { id ->
                binding.preview.setImageResource(id)
            }

            getInt(R.styleable.LsPreferenceView_previewTint, 1).let { id ->
                when {
                    isInEditMode -> {
                        when (id) {
                            0 -> binding.preview.imageTintList = null
                            1 -> ColorManager.colorById(context, ColorManager.COLOR_TEXT_PRIMARY, context.isNightMode())?.let { color ->
                                binding.preview.setTint(color)
                            }
                            2 -> ColorManager.colorById(context, ColorManager.COLOR_TEXT_SECONDARY, context.isNightMode())?.let { color ->
                                binding.preview.setTint(color)
                            }
                            3 -> ColorManager.colorById(context, ColorManager.COLOR_TEXT_TERTIARY, context.isNightMode())?.let { color ->
                                binding.preview.setTint(color)
                            }
                            else -> { }
                        }
                    }

                    else -> {
                        when (id) {
                            0 -> binding.preview.imageTintList = null
                            1 -> binding.preview.setTint(colorManager.theme().colorById(ColorManager.COLOR_TEXT_PRIMARY))
                            2 -> binding.preview.setTint(colorManager.theme().colorById(ColorManager.COLOR_TEXT_SECONDARY))
                            3 -> binding.preview.setTint(colorManager.theme().colorById(ColorManager.COLOR_TEXT_TERTIARY))
                            else -> { }
                        }
                    }
                }
            }

        }
    }

    fun clicks(callback: () -> Unit) {
        binding.viewClicks.clicks { callback() }
    }

    fun getBinding() = binding

    fun setNewChecked(isChecked: Boolean) {
        if (viewWidgetFrame != null) {
            WidgetPreferenceSwitchBinding.bind(viewWidgetFrame!!).viewSwitch.isChecked = isChecked
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

    private fun updateUi(animate: Boolean) {
        val color = colorManager.theme().colorById(backgroundTintAttr)
        when {
            color == null -> binding.viewClicks.setCardBackgroundColor(Color.TRANSPARENT)
            else -> {
                if (animate) {
                    valueAnimator = binding.viewClicks.animCardBackgroundColorChange(endColor = color)
                } else {
                    binding.viewClicks.setCardBackgroundColor(ColorStateList.valueOf(color))
                }
            }
        }
    }

    override fun onThemeChanged(withAnim: Boolean) {
        updateUi(withAnim)
    }

    override fun onLanguageChanged(withAnim: Boolean?) {
        if (primary != -1) binding.primary.text = context.getString(primary)
        if (secondary != -1) binding.secondary.text = context.getString(secondary)
        if (tertiary != -1) binding.tertiary.text = context.getString(tertiary)
    }

}