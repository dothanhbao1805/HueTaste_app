package com.example.huetaste.feature.splash

import android.animation.ObjectAnimator
import android.view.animation.DecelerateInterpolator
import androidx.activity.viewModels
import androidx.core.animation.doOnEnd
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.example.huetaste.databinding.ActivitySplashBinding
import com.example.huetaste.common.extension.startLoginActivity
import com.example.huetaste.common.extension.startMainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SplashActivity : LsActivity<ActivitySplashBinding>(ActivitySplashBinding::inflate) {
    private val viewModel: SplashViewModel by viewModels()

    private var progressAnimator: ObjectAnimator? = null

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        progressAnimator = ObjectAnimator.ofInt(binding.splashProgress, PROGRESS, START_PROGRESS, WAITING_PROGRESS).apply {
            duration = PROGRESS_MS
            interpolator = DecelerateInterpolator()
            start()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.destination.collect(::finishAndOpen)
            }
        }
    }

    /** Fills the progress bar, then opens the screen the user belongs on and removes the splash from the back stack. */
    private fun finishAndOpen(destination: SplashDestination) {
        progressAnimator?.cancel()
        ObjectAnimator.ofInt(binding.splashProgress, PROGRESS, binding.splashProgress.progress, FULL_PROGRESS).apply {
            duration = FILL_MS
            doOnEnd {
                if (isFinishing) return@doOnEnd
                when (destination) {
                    SplashDestination.MAIN -> startMainActivity()
                    SplashDestination.LOGIN -> startLoginActivity()
                }
                finish()
            }
            start()
        }
    }

    override fun onDestroy() {
        progressAnimator?.cancel()
        super.onDestroy()
    }

    private companion object {
        const val PROGRESS = "progress"
        const val START_PROGRESS = 30
        const val WAITING_PROGRESS = 92
        const val FULL_PROGRESS = 100
        const val PROGRESS_MS = 1_100L
        const val FILL_MS = 180L
    }
}
