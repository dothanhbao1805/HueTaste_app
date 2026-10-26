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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.base.R
import com.base.common.extension.animBackgroundColorChange
import com.base.common.extension.clicks
import com.base.common.util.theme.ColorManager
import com.base.common.util.theme.UIChangedListener
import com.base.common.util.theme.UIManager
import com.base.common.util.theme.ViewStyler
import com.base.data.Prefs
import com.base.databinding.BottomBarBinding
import com.base.databinding.ItemInBottomBarBinding
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import androidx.core.content.withStyledAttributes
import com.base.common.extension.isNightMode
import com.base.common.extension.setTint

@SuppressLint("CustomViewStyleable")
@AndroidEntryPoint
class LsBottomBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs), UIChangedListener {

    companion object {
        private const val DEFAULT_SCALE = 0.9f
        private const val DEFAULT_ANIM_DURATION = 250L
        private const val DEFAULT_WEIGHT = 1f
        private val DEFAULT_ITEMS = listOf<Item>()
    }

    @Inject lateinit var colorManager: ColorManager
    @Inject lateinit var prefs: Prefs

    data class Item(@DrawableRes val icon: Int, @DrawableRes val iconActivated: Int, @StringRes val display: Int, val index: Int) {
        var textColor = ColorManager.COLOR_TEXT_SECONDARY
        var textColorActivated = ColorManager.COLOR_TEXT_PRIMARY
        var iconColor = ColorManager.COLOR_TEXT_SECONDARY
        var iconColorActivated = ColorManager.COLOR_ON_THEME
        var weight = DEFAULT_WEIGHT
        var isScale = true
    }

    private var valueAnimator: ValueAnimator? = null
    private var backgroundTintAttr = ColorManager.COLOR_NONE
    private var colorIndicatorAttr = ColorManager.COLOR_NONE
    private var hadBackground = false
    private val binding = BottomBarBinding.inflate(LayoutInflater.from(context), this, true)

    private var itemViews = hashMapOf<Int?, ItemInBottomBarBinding>()
    val itemClicks: Subject<Item> = PublishSubject.create()
    var items: List<Item?> = listOf()
        set(value) {
            itemSelected = value.firstOrNull()
            field = value
            initView()
        }
    var itemSelected: Item? = null
        set(value) {
            if (field != value) {
                field?.let { item ->
                    itemViews[item.index]?.run {
                        if (item.isScale) {
                            this.root.animate().scaleX(DEFAULT_SCALE).scaleY(DEFAULT_SCALE).setDuration(DEFAULT_ANIM_DURATION).start()
                        }
                        this.viewIndicator.setCardBackgroundColor(Color.TRANSPARENT)
                        this.preview.setImageResource(item.icon)
                        colorManager.theme().colorById(item.textColor)?.let { color ->
                            this.display.setTextColor(color)
                        }
                        colorManager.theme().colorById(item.iconColor)?.let { color ->
                            this.preview.setTint(color)
                        }
                    }
                }
                value?.let { item ->
                    itemViews[item.index]?.run {
                        if (item.isScale) {
                            this.root.animate().scaleX(1f).scaleY(1f).setDuration(DEFAULT_ANIM_DURATION).start()
                        }
                        this.preview.setImageResource(item.iconActivated)
                        colorManager.theme().colorById(colorIndicatorAttr)?.let { color ->
                            this.viewIndicator.setCardBackgroundColor(color)
                        }
                        colorManager.theme().colorById(item.textColorActivated)?.let { color ->
                            this.display.setTextColor(color)
                        }
                        colorManager.theme().colorById(item.iconColorActivated)?.let { color ->
                            this.preview.setTint(color)
                        }
                    }
                }
            }
            field = value
        }

    init {
        context.withStyledAttributes(attrs, intArrayOf(android.R.attr.background)) {
            hadBackground = getResourceId(0, 0) != 0
        }

        context.withStyledAttributes(attrs, R.styleable.LsBottomBar) {
            backgroundTintAttr = getInt(R.styleable.LsBottomBar_bgColorTint, ColorManager.COLOR_NONE)
            colorIndicatorAttr = getInt(R.styleable.LsBottomBar_colorIndicator, ColorManager.COLOR_NONE)
        }

        if (!hadBackground && backgroundTintAttr != ColorManager.COLOR_NONE){
            binding.root.setBackgroundColor(Color.WHITE)
        }

        if (isInEditMode){
            items = DEFAULT_ITEMS
        }

        when {
            isInEditMode -> ViewStyler.applyAttributesView(binding.root, attrs, hadBackground, context.isNightMode())
            else -> updateUi()
        }
    }

    private fun initView() {
        binding.viewGroup.removeAllViews().also {
            this.itemViews.clear()
        }

        if (isInEditMode) {
            (0..2).forEach { _ ->
                ItemInBottomBarBinding.inflate(LayoutInflater.from(context), binding.viewGroup, true).apply {
                    this.root.updateLayoutParams<LinearLayout.LayoutParams> {
                        this.weight = DEFAULT_WEIGHT
                    }
                }
            }
            return
        }

        val colorIndicator =  colorManager.theme().colorById(colorIndicatorAttr)

        items.forEachIndexed { _, item ->
            val view = ItemInBottomBarBinding.inflate(LayoutInflater.from(context), binding.viewGroup, true).apply {
                this.root.updateLayoutParams<LinearLayout.LayoutParams> {
                    this.weight = item?.weight ?: DEFAULT_WEIGHT
                }
            }
            val isActivated = item?.index == itemSelected?.index
            val textColorActivated = colorManager.theme().colorById(item?.textColorActivated) ?: colorManager.theme().textPrimary
            val iconColorActivated = colorManager.theme().colorById(item?.iconColorActivated) ?: colorManager.theme().onToolsTheme
            val textColorUnactivated = colorManager.theme().colorById(item?.textColor) ?: colorManager.theme().textSecondary
            val iconColorUnactivated = colorManager.theme().colorById(item?.iconColor) ?: colorManager.theme().textTertiary

            if (item != null) {
                view.root.scaleX = if (item.isScale && isActivated) 1f else DEFAULT_SCALE
                view.root.scaleY = if (item.isScale && isActivated) 1f else DEFAULT_SCALE
                view.viewIndicator.setCardBackgroundColor(if (isActivated) colorIndicator ?: Color.TRANSPARENT else Color.TRANSPARENT)
                view.preview.setImageDrawable(ContextCompat.getDrawable(context, if (isActivated) item.iconActivated else item.icon)?.apply {
                    this.setTint(if (isActivated) iconColorActivated else iconColorUnactivated)
                })
                view.display.text = context.getString(item.display)
                view.display.setTextColor(if (isActivated) textColorActivated else textColorUnactivated)
                view.root.clicks { itemClicks.onNext(item) }
            }

            view.viewIndicator.visibility = if (item == null) View.INVISIBLE else View.VISIBLE
            view.root.visibility = if (item == null) View.INVISIBLE else View.VISIBLE

            itemViews[item?.index] = view
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
        colorManager.theme().colorById(backgroundTintAttr)?.let { color ->
            if (animate) {
                valueAnimator = binding.root.animBackgroundColorChange(endColor = color)
            } else {
                binding.root.backgroundTintList = ColorStateList.valueOf(color)
            }
        }
        itemViews.forEach { (index, view) ->
            val colorIndicator =  colorManager.theme().colorById(colorIndicatorAttr)
            val isActivated = index == itemSelected?.index
            val item = items.getOrNull(index ?: -1)

            item?.let {
                view.viewIndicator.setCardBackgroundColor(if (isActivated) colorIndicator ?: Color.TRANSPARENT else Color.TRANSPARENT)
                view.preview.setImageDrawable(ContextCompat.getDrawable(context, if (isActivated) item.iconActivated else item.icon)?.apply {
                    when {
                        isActivated -> colorManager.theme().colorById(item.iconColorActivated)?.let { color ->
                            this.setTint(color)
                        }
                        else -> colorManager.theme().colorById(item.iconColor)?.let { color ->
                            this.setTint(color)
                        }
                    }
                })
            }

            view.viewIndicator.visibility = if (item == null) View.INVISIBLE else View.VISIBLE
            view.root.visibility = if (item == null) View.INVISIBLE else View.VISIBLE
        }
    }

    override fun onThemeChanged(withAnim: Boolean) {
        updateUi(withAnim)
        super.onThemeChanged(withAnim)
    }

}