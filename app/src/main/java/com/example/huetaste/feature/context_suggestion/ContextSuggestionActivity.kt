package com.example.huetaste.feature.context_suggestion

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.WindowCompat
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.example.huetaste.databinding.ActivityContextSuggestionBinding
import com.example.huetaste.feature.dish_detail.DishDetailActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContextSuggestionActivity : LsActivity<ActivityContextSuggestionBinding>(ActivityContextSuggestionBinding::inflate) {
    private var showPlaces = false
    private var selectedHearts = DEFAULT_HEARTS.toMutableSet()

    private val hearts get() = listOf(
        binding.heartBunBo,
        binding.heartBanhBeo,
        binding.heartComHen,
        binding.heartNamPho
    )

    private val dishCards get() = listOf(
        binding.dishBunBoCard,
        binding.dishBanhBeoCard,
        binding.dishComHenCard,
        binding.dishNamPhoCard
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        savedInstanceState?.let { state ->
            showPlaces = state.getBoolean(KEY_SHOW_PLACES, false)
            selectedHearts = state.getIntArray(KEY_HEARTS)?.toMutableSet() ?: DEFAULT_HEARTS.toMutableSet()
        }
        // viewCreated() runs inside LsActivity.onCreate, so restored state must be read first.
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.backButton.clicks { handleBackPressed() }

        showTab(showPlaces)
        binding.tabDishes.clicks(withAnim = false) { showTab(false) }
        binding.tabPlaces.clicks(withAnim = false) { showTab(true) }
        binding.dishDetailsButton.clicks { openDishDetail() }
        dishCards.forEach { card -> card.clicks { openDishDetail() } }

        hearts.forEachIndexed { index, heart ->
            heart.isSelected = index in selectedHearts
            heart.clicks { toggleHeart(index, heart) }
        }
    }

    // Every card opens the same detail screen until dishes come from real data.
    private fun openDishDetail() {
        startActivity(DishDetailActivity.newIntent(this))
    }

    private fun showTab(places: Boolean) {
        showPlaces = places
        binding.tabDishes.isSelected = !places
        binding.tabPlaces.isSelected = places
    }

    private fun toggleHeart(index: Int, heart: View) {
        val selected = !heart.isSelected
        heart.isSelected = selected
        if (selected) selectedHearts.add(index) else selectedHearts.remove(index)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_SHOW_PLACES, showPlaces)
        outState.putIntArray(KEY_HEARTS, selectedHearts.toIntArray())
    }

    companion object {
        private const val KEY_SHOW_PLACES = "context_show_places"
        private const val KEY_HEARTS = "context_selected_hearts"
        private val DEFAULT_HEARTS = setOf(0)

        fun newIntent(context: Context): Intent = Intent(context, ContextSuggestionActivity::class.java)
    }
}
