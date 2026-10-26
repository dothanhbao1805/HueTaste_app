package com.base.common.base

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.base.R
import com.base.common.extension.lightNavigationBar
import com.base.common.extension.lightStatusBar
import com.base.common.extension.makeSystemBarsTransparent
import javax.annotation.Nullable

abstract class LsDialogFragment<VB : ViewBinding>(
    val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB,
    val isFullScreen: Boolean,
) : DialogFragment() {

    lateinit var binding: VB

    var autoColorSystemBar = true

    abstract fun onViewCreated()

    override fun onCreate(@Nullable savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFullScreen) {
            setStyle(STYLE_NO_TITLE, R.style.FullScreenDialogStyle)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(LayoutParams.MATCH_PARENT, if (isFullScreen) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            when {
                isFullScreen -> dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                else -> {
                    setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                    setDimAmount(0.7f)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (isFullScreen && autoColorSystemBar){
            dialog?.window?.statusBarColor = LsApplication.app.colorManager.theme().background
            dialog?.window?.lightStatusBar()
            dialog?.window?.navigationBarColor = LsApplication.app.colorManager.theme().background
            dialog?.window?.lightNavigationBar()
        }

        onViewCreated()
    }

}
