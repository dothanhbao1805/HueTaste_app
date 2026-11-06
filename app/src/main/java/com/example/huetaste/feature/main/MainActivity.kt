package com.example.huetaste.feature.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.base.common.base.LsActivity
import com.base.common.base.LsPageAdapter
import com.base.common.widget.scale.PushDownAnim
import com.base.common.extension.clicks
import com.example.huetaste.common.extension.startAiChatActivity
import com.example.huetaste.common.extension.startLoginActivity
import com.example.huetaste.databinding.ActivityMainBinding
import com.example.huetaste.domain.model.AuthState
import com.example.huetaste.domain.usecase.GetAuthStateUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : LsActivity<ActivityMainBinding>(ActivityMainBinding::inflate) {
    @Inject lateinit var getAuthState: GetAuthStateUseCase

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
        // Tabs change only through the bottom navigation, not by horizontal swipe.
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(pageCallback)
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val tab = MainTab.entries.firstOrNull { it.menuItemId == item.itemId }
                ?: return@setOnItemSelectedListener false
            if (binding.viewPager.currentItem != tab.ordinal) {
                binding.viewPager.setCurrentItem(tab.ordinal, true)
            }
            true
        }
        // Menu items are inflated by BottomNavigationView, so they get the press animation directly; their click
        // handling stays with the menu.
        MainTab.entries.forEach { tab ->
            binding.bottomNavigation.findViewById<View>(tab.menuItemId)?.let(PushDownAnim::setPushDownAnimTo)
        }
        // The AI assistant is its own Activity, not a tab.
        binding.centerAiButton.clicks { startAiChatActivity() }
        selectMenuItem(binding.viewPager.currentItem)
        returnToLoginWhenSessionEnds()
    }

    /**
     * A signed-in user whose session the backend no longer accepts (the refresh token expired or the account was
     * blocked) goes back to the login. The state is read again whenever the screen starts, since the session may
     * have ended while another screen was on top.
     */
    private fun returnToLoginWhenSessionEnds() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (getAuthState() == AuthState.SIGNED_OUT) {
                    openLogin()
                    return@repeatOnLifecycle
                }
                getAuthState.observe().collect { state -> if (state == AuthState.SIGNED_OUT) openLogin() }
            }
        }
    }

    private fun openLogin() {
        startLoginActivity()
        finish()
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
    }

    override fun onDestroy() {
        binding.bottomNavigation.setOnItemSelectedListener(null)
        binding.viewPager.unregisterOnPageChangeCallback(pageCallback)
        super.onDestroy()
    }

    companion object {
        /** Opens the main screen as the only screen, after the launch screen or a successful login. */
        fun newRootIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}
