package com.base.common.base

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.viewbinding.ViewBinding
import com.base.R
import com.base.common.extension.lightNavigationBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class LsBottomSheet<VB : ViewBinding>(
    val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : BottomSheetDialogFragment() {

    lateinit var binding: VB

    abstract fun isFull(): Boolean
    abstract fun isAdjustResize(): Boolean
    abstract fun onViewCreated()


    open fun isShowNavigationBar(): Boolean = true
    open fun isCancelableOutside(): Boolean = true
    open fun isDraggable(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            if (isAdjustResize()) {
                R.style.BottomSheetFragmentStyleAdjustResize
            } else {
                R.style.BottomSheetFragmentStyle
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = bindingInflater(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        setupNavigationBar()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupNavigationBar()
        onViewCreated()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), theme)

        bottomSheetDialog.setCanceledOnTouchOutside(isCancelableOutside())
        bottomSheetDialog.setCancelable(isCancelableOutside())

        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener

            setupNavigationBar()
            removeBottomNavigationGap(bottomSheet)

            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.skipCollapsed = true
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.isDraggable = isDraggable()

            if (isFull()) {
                bottomSheet.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            } else {
                behavior.isFitToContents = true
                bottomSheet.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
        }

        return bottomSheetDialog
    }
    private fun setupNavigationBar() {
        val window = dialog?.window ?: return
        val controller = WindowInsetsControllerCompat(window, window.decorView)

        if (isShowNavigationBar()) {
            WindowCompat.setDecorFitsSystemWindows(window, true)

            controller.show(WindowInsetsCompat.Type.navigationBars())
            window.navigationBarColor = requireContext().getColor(R.color.background_main)
            window.lightNavigationBar()
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)

            window.navigationBarColor = Color.WHITE
            window.statusBarColor = Color.TRANSPARENT

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            window.decorView.setBackgroundColor(Color.TRANSPARENT)

            controller.hide(WindowInsetsCompat.Type.navigationBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            binding.root.fitsSystemWindows = false
            binding.root.updatePadding(bottom = 0)

            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
                view.updatePadding(bottom = 0)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    private fun removeBottomNavigationGap(bottomSheet: View) {
        if (isShowNavigationBar()) return

        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        bottomSheet.fitsSystemWindows = false
        bottomSheet.updatePadding(bottom = 0)
        bottomSheet.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = 0
        }

        (bottomSheet.parent as? View)?.apply {
            setBackgroundColor(Color.TRANSPARENT)
            fitsSystemWindows = false
            updatePadding(bottom = 0)
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = 0
            }

            ViewCompat.setOnApplyWindowInsetsListener(this) { view, _ ->
                view.updatePadding(bottom = 0)
                WindowInsetsCompat.CONSUMED
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { view, _ ->
            view.updatePadding(bottom = 0)
            WindowInsetsCompat.CONSUMED
        }
    }
}
