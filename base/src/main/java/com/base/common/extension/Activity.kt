package com.base.common.extension

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.base.domain.model.DeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun Activity.hideKeyboard() {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    // Lấy view hiện tại đang focus
    var view: View? = currentFocus
    if (view == null) {
        view = View(this)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Activity.hideKeyboard(view: View) {
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.showKeyboard(view: View) {
    view.requestFocus()

    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}



fun Activity.getDeviceType(): DeviceType {
    val windowInfoTracker = WindowInfoTracker.getOrCreate(this)
    val windowLayoutInfo = runBlocking {
        windowInfoTracker.windowLayoutInfo(this@getDeviceType).first()
    }

    val foldingFeature = windowLayoutInfo.displayFeatures
        .filterIsInstance<FoldingFeature>()
        .firstOrNull()

    if (foldingFeature != null) {
        return DeviceType.FOLDABLE
    }

    // 2. Kiểm tra tablet (sw >= 600dp)
    val configuration = this.resources.configuration
    val smallestScreenWidthDp = configuration.smallestScreenWidthDp
    if (smallestScreenWidthDp >= 600) {
        return DeviceType.TABLET
    }

    // 3. Ngược lại → phone
    return DeviceType.PHONE
}

fun Activity.isDeviceFoldable(): Boolean = runBlocking {
    val windowInfoTracker = WindowInfoTracker.getOrCreate(this@isDeviceFoldable)
    val windowLayoutInfo = withContext(Dispatchers.IO) {
        windowInfoTracker.windowLayoutInfo(this@isDeviceFoldable).first()
    }

    return@runBlocking windowLayoutInfo.displayFeatures.any { feature ->
        feature is FoldingFeature && (feature.state == FoldingFeature.State.HALF_OPENED || feature.state == FoldingFeature.State.FLAT)
    }
}

fun View.applySystemBarInsets(systemBarInsets: (statusBarHeight: Int, navigationBarHeight: Int, keyboardVisibilityChanged: Pair<Boolean, Int>) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom

        val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
        val isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
        val keyboardHeight = imeInsets.bottom

        systemBarInsets(statusBarHeight, navBarHeight, isKeyboardVisible to keyboardHeight)
        insets
    }

    if (isAttachedToWindow) {
        requestApplyInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                v.removeOnAttachStateChangeListener(this)
                v.requestApplyInsets()
            }

            override fun onViewDetachedFromWindow(v: View) {}
        })
    }
}

fun Window.makeSystemBarsTransparent() {
    this.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION // Đặt layout kéo dài xuống navigation bar
            )
    this.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    this.statusBarColor = Color.TRANSPARENT
    this.navigationBarColor = Color.TRANSPARENT
}

fun Activity.transparent(hideNavigationBar: Boolean = true) {
    window.decorView.systemUiVisibility = when {
        hideNavigationBar -> View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        else -> View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
}

fun Activity.transparentStatus(hideNavigationBar: Boolean = true) {
    window.decorView.systemUiVisibility = when {
        hideNavigationBar ->
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        else ->
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
}

fun Activity.transparentNew(hideNavigationBar: Boolean = true) {

    WindowCompat.setDecorFitsSystemWindows(window, false)

    val controller = WindowCompat.getInsetsController(window, window.decorView)

    if (hideNavigationBar) {
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    controller.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
}

fun Activity.showNavigationBar() {
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
}

fun Activity.lightStatusBar() {
    val insets = WindowCompat.getInsetsController(window, window.decorView)
    insets.isAppearanceLightStatusBars = true
}

fun Activity.darkStatusBar() {
    val insets = WindowCompat.getInsetsController(window, window.decorView)
    insets.isAppearanceLightStatusBars = false
}

fun Activity.lightNavigationBar() {
    val insets = WindowCompat.getInsetsController(window, window.decorView)
    insets.isAppearanceLightNavigationBars = true
}

fun Activity.darkNavigationBar() {
    val insets = WindowCompat.getInsetsController(window, window.decorView)
    insets.isAppearanceLightNavigationBars = false
}

fun Window.lightStatusBar() {
    val insets = WindowCompat.getInsetsController(this, this.decorView)
    insets.isAppearanceLightStatusBars = true
}

fun Window.darkStatusBar() {
    val insets = WindowCompat.getInsetsController(this, this.decorView)
    insets.isAppearanceLightStatusBars = false
}

fun Window.lightNavigationBar() {
    val insets = WindowCompat.getInsetsController(this, this.decorView)
    insets.isAppearanceLightNavigationBars = true
}

fun Window.darkNavigationBar() {
    val insets = WindowCompat.getInsetsController(this, this.decorView)
    insets.isAppearanceLightNavigationBars = false
}

private fun Activity.setWindowFlag(bits: Int, on: Boolean = false) {
    val win = window
    val winParams = win.attributes
    if (on) {
        winParams.flags = winParams.flags or bits
    } else {
        winParams.flags = winParams.flags and bits.inv()
    }
    win.attributes = winParams
}

fun Activity.hideSystemBars() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.hide(
            WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
        )
        window.insetsController?.systemBarsBehavior =
            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    window.statusBarColor = android.graphics.Color.TRANSPARENT
    window.navigationBarColor = android.graphics.Color.TRANSPARENT
}