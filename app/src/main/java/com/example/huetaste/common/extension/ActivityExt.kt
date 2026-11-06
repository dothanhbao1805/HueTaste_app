package com.example.huetaste.common.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.base.R as BaseR
import com.example.huetaste.feature.ai_chat.AiChatActivity
import com.example.huetaste.feature.ai_chat.ChatHistoryActivity
import com.example.huetaste.feature.auth.LoginActivity
import com.example.huetaste.feature.auth.RegisterActivity
import com.example.huetaste.feature.context_suggestion.ContextSuggestionActivity
import com.example.huetaste.feature.dish_detail.DishDetailActivity
import com.example.huetaste.feature.main.MainActivity
import com.example.huetaste.feature.place_detail.PlaceDetailActivity
import com.example.huetaste.feature.scan.ScanActivity
import com.example.huetaste.feature.taste_setup.TasteSetupActivity

/**
 * The single place that starts Activities. Every screen, in-app or system (settings, dialer, share sheet, maps),
 * slides in from the right while the previous one leaves to the left.
 */

// region In-app screens

fun Context.startMainActivity() = startRootActivityRtl(MainActivity.newRootIntent(this))

fun Context.startLoginActivity() = startRootActivityRtl(LoginActivity.newRootIntent(this))

fun Context.startRegisterActivity() = startActivityRtl(RegisterActivity.newIntent(this))

fun Context.startTasteSetupActivity() = startRootActivityRtl(TasteSetupActivity.newRootIntent(this))

fun Context.startContextSuggestionActivity() = startActivityRtl(ContextSuggestionActivity.newIntent(this))

fun Context.startScanActivity() = startActivityRtl(ScanActivity.newIntent(this))

fun Context.startDishDetailActivity() = startActivityRtl(DishDetailActivity.newIntent(this))

fun Context.startDishDetailActivity(dishId: String) = startActivityRtl(DishDetailActivity.newIntent(this, dishId))

fun Context.startScanDishDetailActivity(scanId: String) = startActivityRtl(DishDetailActivity.newScanIntent(this, scanId))

fun Context.startPlaceDetailActivity(placeId: String) = startActivityRtl(PlaceDetailActivity.newIntent(this, placeId))

/** Opens the assistant on [conversationId], or on a new conversation; an open chat is brought back to the front. */
fun Context.startAiChatActivity(conversationId: String? = null) =
    startActivityRtl(AiChatActivity.newIntent(this, conversationId))

fun Context.startChatHistoryActivity() = startActivityRtl(ChatHistoryActivity.newIntent(this))

// endregion

// region System screens

/** Opens this app's page in system settings. */
fun Context.startAppSettings() {
    startActivityRtl(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
}

/** Opens the dialer with [phone] filled in. Throws [android.content.ActivityNotFoundException] without a dialer. */
fun Context.startDialer(phone: String) {
    startActivityRtl(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))))
}

/** Opens the system share sheet for plain [text]. */
fun Context.startShareText(text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivityRtl(Intent.createChooser(send, chooserTitle))
}

/** Opens [uri] in the app that handles it. Throws [android.content.ActivityNotFoundException] when none does. */
fun Context.startViewUri(uri: Uri) {
    startActivityRtl(Intent(Intent.ACTION_VIEW, uri))
}

// endregion

/**
 * Opens a root screen ([intent] built with NEW_TASK | CLEAR_TASK) as the only screen of the task.
 *
 * From an Activity, CLEAR_TASK would destroy the current screen before the new one has drawn, so the transition
 * runs over an empty task and shows a black frame. Instead the new screen is started normally with the slide, and the
 * screens below are finished with [Activity.finishAffinity], which gives the same back stack without the flash.
 */
private fun Context.startRootActivityRtl(intent: Intent) {
    val activity = this as? Activity
    if (activity == null) {
        startActivity(intent)
        return
    }
    intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivityRtl(intent)
    activity.finishAffinity()
}

/** Starts [intent] with the right-to-left transition when this context is an Activity. */
fun Context.startActivityRtl(intent: Intent) {
    val activity = this as? Activity
    if (activity == null) {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }
    // Android 14+ reads the override when the launch is requested, so it must be registered before startActivity.
    // Older versions apply overridePendingTransition to the launch that was just requested, so it goes after.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        activity.overrideActivityTransition(
            Activity.OVERRIDE_TRANSITION_OPEN,
            BaseR.anim.slide_in_right,
            BaseR.anim.slide_out_left,
        )
        activity.startActivity(intent)
    } else {
        activity.startActivity(intent)
        @Suppress("DEPRECATION")
        activity.overridePendingTransition(BaseR.anim.slide_in_right, BaseR.anim.slide_out_left)
    }
}
