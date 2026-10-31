package com.example.huetaste.feature.dish_detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.databinding.ActivityDishDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
    class DishDetailActivity : LsActivity<ActivityDishDetailBinding>(ActivityDishDetailBinding::inflate) {
    private var isFavorite = true

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let { isFavorite = it.getBoolean(KEY_FAVORITE, true) }
        // viewCreated() runs inside LsActivity.onCreate, so restored state must be read first.
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }
        binding.shareButton.clicks { shareDish() }

        // The header and hero hearts show the same favorite state.
        showFavorite()
        binding.headerFavoriteButton.clicks { toggleFavorite() }
        binding.heroFavoriteButton.clicks { toggleFavorite() }
    }

    private fun toggleFavorite() {
        isFavorite = !isFavorite
        showFavorite()
    }

    private fun showFavorite() {
        binding.headerFavoriteButton.isSelected = isFavorite
        binding.heroFavoriteButton.isSelected = isFavorite
    }

    private fun shareDish() {
        val text = getString(R.string.dish_share_text, getString(R.string.dish_name), getString(R.string.dish_story_tip))
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, getString(R.string.dish_share_chooser)))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FAVORITE, isFavorite)
    }

    companion object {
        private const val KEY_FAVORITE = "dish_favorite"

        fun newIntent(context: Context): Intent = Intent(context, DishDetailActivity::class.java)
    }
}
