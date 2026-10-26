package com.base.common.base

import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Guideline
import androidx.viewbinding.ViewBinding
import com.base.R
import com.base.common.Config
import com.base.common.extension.applySystemBarInsets
import com.base.common.extension.darkNavigationBar
import com.base.common.extension.darkStatusBar
import com.base.common.extension.lightNavigationBar
import com.base.common.extension.lightStatusBar
import com.base.common.extension.makeSystemBarsTransparent
import com.base.common.extension.transparent
import com.base.common.util.theme.ColorManager
import com.base.common.util.theme.UIManager
import com.base.data.Prefs
import com.uber.autodispose.android.lifecycle.scope
import com.uber.autodispose.autoDispose
import javax.inject.Inject

abstract class LsActivity<VB : ViewBinding>(
    val bindingInflater: (LayoutInflater) -> VB
): AppCompatActivity() {

    @Inject lateinit var prefs: Prefs
    @Inject lateinit var colorManager: ColorManager
    @Inject lateinit var config: Config

    open val activityConfig: ActivityConfig = ActivityConfig()
    val binding: VB by lazy { bindingInflater(layoutInflater) }
    var isKeyboardVisible = false
    var statusBarHeight: Int = 0
    var navigationBarHeight: Int = 0

    abstract fun viewCreated()
    open fun onKeyboardHeightChanged(keyboardHeight: Int) { }
    open fun handleBackPressed() { finish() }
    open fun isAutoHideKeyboard(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        if (activityConfig.isTransparentSystemBar) window.makeSystemBarsTransparent()
        lightStatusBar()
        lightNavigationBar()

        if (activityConfig.lockRotation) {
            requestedOrientation = activityConfig.lockedOrientation
        }
        
        super.onCreate(savedInstanceState)
//        themeChanged(prefs.themeId.get())
        setContentView(binding.root)

        binding.root.applySystemBarInsets { statusBarHeight, navigationBarHeight, (isKeyboardVisible, keyboardHeight) ->
            if (activityConfig.autoMarginStatusBar) findViewById<Guideline>(R.id.statusBar)?.setGuidelineBegin(statusBarHeight)
            if (activityConfig.autoMarginNavigationBar) findViewById<Guideline>(R.id.navigationBar)?.setGuidelineEnd(navigationBarHeight)
            this.isKeyboardVisible = isKeyboardVisible
            this.statusBarHeight = statusBarHeight
            this.navigationBarHeight = navigationBarHeight
            this.onKeyboardHeightChanged(keyboardHeight)
        }

        onBackPressedDispatcher.addCallback(this) {
            handleBackPressed()
        }

        viewCreated()
        initSetDisableScreenShot()

        prefs.themeId
            .asObservable()
            .skip(1)
            .distinctUntilChanged()
            .autoDispose(scope())
            .subscribe { themeId ->
//                themeChanged(themeId)
            }

    }

    private fun initSetDisableScreenShot() {

    }

    private fun themeChanged(themeId: Int) {
        if (activityConfig.isAutoChangeColorStatusBar) {
            if (prefs.isNightMode()) darkStatusBar() else lightStatusBar()
            colorManager.theme().colorById(activityConfig.statusBarColor)?.let { color ->
                window.statusBarColor = color
            }
        }
        if (activityConfig.isAutoChangeColorNavigationBar) {
            if (prefs.isNightMode()) darkNavigationBar() else lightNavigationBar()
            colorManager.theme().colorById(activityConfig.navigationBarColor)?.let { color ->
                window.navigationBarColor = color
            }
        }

        colorManager.reload(themeId = themeId).also {
            UIManager.themeId = themeId
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (!isAutoHideKeyboard()) return super.dispatchTouchEvent(ev)

        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val currentFocus = currentFocus

            if (currentFocus is EditText) {
                val outRect = Rect()
                currentFocus.getGlobalVisibleRect(outRect)

                val isTouchInsideEditText = outRect.contains(
                    ev.rawX.toInt(),
                    ev.rawY.toInt()
                )

                if (!isTouchInsideEditText) {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
                    currentFocus.clearFocus()
                }
            }
        }

        return super.dispatchTouchEvent(ev)
    }

    class ActivityConfig {
        var isTransparentSystemBar = true
        var autoMarginStatusBar = true
        var autoMarginNavigationBar = true
        var isAutoChangeColorStatusBar = true
        var isAutoChangeColorNavigationBar = true
        var statusBarColor = ColorManager.COLOR_BACKGROUND
        var navigationBarColor = ColorManager.COLOR_BACKGROUND
        var lockRotation = true
        var lockedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

}
