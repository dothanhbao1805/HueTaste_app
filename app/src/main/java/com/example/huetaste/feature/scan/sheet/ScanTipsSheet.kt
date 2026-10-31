package com.example.huetaste.feature.scan.sheet

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.base.common.base.LsBottomSheet
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ItemScanTipBinding
import com.example.huetaste.databinding.SheetScanTipsBinding

class ScanTipsSheet : LsBottomSheet<SheetScanTipsBinding>(SheetScanTipsBinding::inflate) {

    override fun isFull(): Boolean = false

    override fun isAdjustResize(): Boolean = false

    override fun onViewCreated() {
        binding.closeButton.clicks { dismiss() }
        Tip.entries.forEach { tip ->
            val item = ItemScanTipBinding.inflate(layoutInflater, binding.tipsContainer, true)
            item.tipIcon.setImageResource(tip.icon)
            item.tipText.setText(tip.text)
        }
    }

    private enum class Tip(@param:DrawableRes val icon: Int, @param:StringRes val text: Int) {
        LIGHT(R.drawable.ic_wb_sunny, R.string.scan_tip_light),
        FRAME(R.drawable.ic_photo_camera, R.string.scan_tip_frame),
        ANGLE(R.drawable.ic_ramen_dining, R.string.scan_tip_angle),
        SINGLE(R.drawable.ic_restaurant, R.string.scan_tip_single),
    }

    companion object {
        private const val TAG = "scan_tips"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            ScanTipsSheet().show(fragmentManager, TAG)
        }
    }
}
