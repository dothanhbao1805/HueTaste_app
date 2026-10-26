package com.base.common.extension

import android.Manifest
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.UnderlineSpan
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.base.common.base.LsApplication
import com.base.common.widget.scale.PushDownAnim
import com.google.android.material.animation.ArgbEvaluatorCompat
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment

fun getMaterialDialogTheme(): Int {
    return if (LsApplication.app.prefs.isNightMode()) com.base.R.style.DialogDark else com.base.R.style.DialogLight
}

fun TextView.makeUnderlines(isUnderlineText: Boolean = true, colorUnderline: Int, vararg texts: String) {
    val originalText = this.text.toString()
    val spannableString = SpannableString(originalText)
    val lowerOriginal = originalText.lowercase()

    for (txt in texts) {
        var searchIndex = 0
        val lowerTarget = txt.lowercase()

        while (searchIndex < lowerOriginal.length) {
            val startIndex = lowerOriginal.indexOf(lowerTarget, searchIndex)
            if (startIndex == -1) break

            val underlineSpan = object : UnderlineSpan() {
                override fun updateDrawState(ds: TextPaint) {
                    ds.isUnderlineText = isUnderlineText
                    ds.color = colorUnderline
                }
            }

            spannableString.setSpan(
                underlineSpan,
                startIndex,
                startIndex + txt.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            searchIndex = startIndex + txt.length
        }
    }

    this.text = spannableString
}


fun TextView.makeLinks(isUnderlineText: Boolean = true, colorUnderline: Int, vararg links: Pair<String, View.OnClickListener>) {
    val spannableString = SpannableString(this.text)
    var startIndexOfLink = -1
    for (link in links) {
        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(textPaint: TextPaint) {
                textPaint.color = colorUnderline
                textPaint.isUnderlineText = isUnderlineText
            }

            override fun onClick(view: View) {
                Selection.setSelection((view as TextView).text as Spannable, 0)
                view.invalidate()
                link.second.onClick(view)
            }
        }
        startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)
        spannableString.setSpan(
            clickableSpan, startIndexOfLink, startIndexOfLink + link.first.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.movementMethod = LinkMovementMethod.getInstance()
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
}

@RequiresPermission(Manifest.permission.VIBRATE)
fun View.vibrate() {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(10L, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(10L)
    }
}

fun View.clicks(debounce: Long = 250, withAnim: Boolean = true, clicks: (View) -> Unit) {
    var lastClickTime: Long = 0
    if (withAnim) {
        animate().cancel()
        scaleX = 1f
        scaleY = 1f
        if (getTag(com.base.R.id.tag_push_down_attached) != true) {
            PushDownAnim.setPushDownAnimTo(this)
            setTag(com.base.R.id.tag_push_down_attached, true)
        }
    }
    setOnClickListener {
        if (SystemClock.elapsedRealtime() - lastClickTime < debounce) return@setOnClickListener
        clicks(this)
        lastClickTime = SystemClock.elapsedRealtime()
    }
}

inline fun <reified T : RecyclerView.ViewHolder> RecyclerView.forEachVisibleHolder(
    action: (T) -> Unit
) {
    for (i in 0 until childCount) {
        action(getChildViewHolder(getChildAt(i)) as T)
    }
}

fun View.setBackgroundTint(color: Int) {
    // API 21 doesn't support this
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
        background?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    backgroundTintList = ColorStateList.valueOf(color)
}

fun View.setPaddingHorizontal(padding: Int) {
    setPadding(padding, paddingTop, padding, paddingBottom)
}

fun View.setPaddingVertical(padding: Int) {
    setPadding(paddingLeft, padding, paddingRight, padding)
}

fun EditText.showKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun EditText.hideKeyboard() {
    requestFocus()
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(windowToken, 0)
}

fun ImageView.setTint(color: Int? = null) {
    if (color == null) {
        imageTintList = null
        return
    }
    imageTintList = ColorStateList.valueOf(color)
}

fun View.visible(){ isVisible = true }

fun View.invisible(){ visibility = View.INVISIBLE }

fun View.gone(){ isVisible = false }

fun View.isClickOnView(x: Float, y: Float): Boolean {
    val outRect = Rect()
    val location = IntArray(2)
    this.getDrawingRect(outRect)
    this.getLocationOnScreen(location)
    return outRect.let {
        outRect.offset(location[0], location[1])
        outRect.contains(x.toInt(), y.toInt())
    }
}

fun View.setRatio(ratio: Float) {
    if (parent is ConstraintLayout) {
        val layout = parent as ConstraintLayout
        val set = ConstraintSet()
        set.clone(layout)

        val ratioString = if (ratio > 0f) "${ratio}:1" else null

        set.setDimensionRatio(this.id, ratioString)
        set.applyTo(layout)
    }
}

fun View.setRatio(width: Int, height: Int) {
    if (parent is ConstraintLayout) {
        val layout = parent as ConstraintLayout
        val set = ConstraintSet()
        set.clone(layout)

        val ratioString = "${width}:$height"

        set.setDimensionRatio(this.id, ratioString)
        set.applyTo(layout)
    }
}

fun View.setRatio(ratioString: String) {
    if (parent is ConstraintLayout) {
        val layout = parent as ConstraintLayout
        val set = ConstraintSet()
        set.clone(layout)

        set.setDimensionRatio(this.id, ratioString)
        set.applyTo(layout)
    }
}
