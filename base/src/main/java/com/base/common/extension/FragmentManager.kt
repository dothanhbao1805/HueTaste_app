package com.base.common.extension

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.base.R

fun FragmentManager.replaceFragment(container: View, fragment: Fragment) {
    val fragmentTransaction = beginTransaction().run {
        this.setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
        this.replace(container.id, fragment)
    }
    when {
        this.isStateSaved -> fragmentTransaction.commitAllowingStateLoss()
        else -> fragmentTransaction.commit()
    }
}