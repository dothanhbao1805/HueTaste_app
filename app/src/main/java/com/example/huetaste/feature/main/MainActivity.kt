package com.example.huetaste.feature.main

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.base.common.base.LsActivity
import com.base.common.base.LsPageAdapter
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : LsActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            selectMenuItem(position)
        }
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // LsActivity already positions the bar above the navigationBar Guideline.
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNavigation) { _, insets -> insets }
        binding.viewPager.adapter = LsPageAdapter(supportFragmentManager, lifecycle).apply {
            addFragments(*MainTab.entries.map { it.createFragment() }.toTypedArray())
        }
        binding.viewPager.offscreenPageLimit = MainTab.entries.size - 1
        binding.viewPager.registerOnPageChangeCallback(pageCallback)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val tab = MainTab.entries.firstOrNull { it.menuItemId == item.itemId }
                ?: return@setOnItemSelectedListener false
            if (binding.viewPager.currentItem != tab.ordinal) {
                binding.viewPager.setCurrentItem(tab.ordinal, true)
            }
            true
        }
        binding.centerAiButton.clicks {
            val aiTab = MainTab.AI_ASSISTANT
            if (binding.viewPager.currentItem != aiTab.ordinal) {
                binding.viewPager.setCurrentItem(aiTab.ordinal, true)
            }
            selectMenuItem(aiTab.ordinal)
        }
        selectMenuItem(binding.viewPager.currentItem)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        selectMenuItem(binding.viewPager.currentItem)
    }

    private fun selectMenuItem(position: Int) {
        val itemId = MainTab.entries[position].menuItemId
        if (binding.bottomNavigation.selectedItemId != itemId) {
            binding.bottomNavigation.selectedItemId = itemId
        }
        binding.centerAiButton.isSelected = (position == MainTab.AI_ASSISTANT.ordinal)
    }

    override fun onDestroy() {
        binding.bottomNavigation.setOnItemSelectedListener(null)
        binding.viewPager.unregisterOnPageChangeCallback(pageCallback)
        super.onDestroy()
    }
}
