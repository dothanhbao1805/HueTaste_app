package com.base.common.extension

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.base.R
import com.base.common.base.LsApplication
import com.base.data.Prefs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

fun BottomSheetDialogFragment.safeDismiss(activity: Activity) {
    if (!isAdded) return
    if (activity !is AppCompatActivity) return

    val manager = activity.supportFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { dismiss() }
    } else {
        activity.lifecycleScope.launchWhenResumed {
            tryOrNull { dismissAllowingStateLoss() }
        }
    }
}

/**
 * Safe dismiss cho BottomSheetDialogFragment từ Fragment
 */
fun BottomSheetDialogFragment.safeDismiss(fragment: Fragment) {
    if (!isAdded) return

    val manager = fragment.childFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { dismiss() }
    } else {
        fragment.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            tryOrNull { dismissAllowingStateLoss() }
        }
    }
}

/**
 * Safe dismiss cho DialogFragment từ Activity
 */
fun DialogFragment.safeDismiss(activity: Activity) {
    if (!isAdded) return
    if (activity !is AppCompatActivity) return

    val manager = activity.supportFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { dismiss() }
    } else {
        activity.lifecycleScope.launchWhenResumed {
            tryOrNull { dismissAllowingStateLoss() }
        }
    }
}

fun DialogFragment.safeDismiss(fragment: Fragment) {
    if (!isAdded) return

    val manager = fragment.childFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { dismiss() }
    } else {
        fragment.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            tryOrNull { dismissAllowingStateLoss() }
        }
    }
}

fun BottomSheetDialogFragment.safeShow(activity: Activity, tag: String? = null) {
    if (isAdded) return
    if (activity !is AppCompatActivity) return

    val manager = activity.supportFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { show(manager, tag ?: this.tag) }
    } else {
        // postpone tới khi activity resumed
        activity.lifecycleScope.launchWhenResumed {
            tryOrNull { show(manager, tag ?: this@safeShow.tag) }
        }
    }
}

/**
 * Safe show cho BottomSheetDialogFragment từ Fragment
 */
fun BottomSheetDialogFragment.safeShow(fragment: Fragment, tag: String? = null) {
    if (isAdded) return

    val manager = fragment.childFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { show(manager, tag ?: this.tag) }
    } else {
        fragment.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            tryOrNull { show(manager, tag ?: this@safeShow.tag) }
        }
    }
}

/**
 * Safe show cho DialogFragment từ Activity
 */
fun DialogFragment.safeShow(activity: Activity, tag: String? = null) {
    if (isAdded) return
    if (activity !is AppCompatActivity) return

    val manager = activity.supportFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { show(manager, tag ?: this.tag) }
    } else {
        activity.lifecycleScope.launchWhenResumed {
            tryOrNull { show(manager, tag ?: this@safeShow.tag) }
        }
    }
}

/**
 * Safe show cho DialogFragment từ Fragment
 */
fun DialogFragment.safeShow(fragment: Fragment, tag: String? = null) {
    if (isAdded) return

    val manager = fragment.childFragmentManager
    if (!manager.isStateSaved) {
        tryOrNull { show(manager, tag ?: this.tag) }
    } else {
        fragment.viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            tryOrNull { show(manager, tag ?: this@safeShow.tag) }
        }
    }
}

fun DialogFragment.show(activity: Activity){
    if (activity !is AppCompatActivity) return
    if (isAdded) return
    tryOrNull { show(activity.supportFragmentManager, tag) }
}