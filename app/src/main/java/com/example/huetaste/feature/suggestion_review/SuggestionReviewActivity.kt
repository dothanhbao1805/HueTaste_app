package com.example.huetaste.feature.suggestion_review

import android.content.Context
import android.content.Intent
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.example.huetaste.R
import com.example.huetaste.common.extension.startContextSuggestionActivity
import com.example.huetaste.common.extension.startDailyNeedsActivity
import com.example.huetaste.common.extension.startDietPreferencesActivity
import com.example.huetaste.common.extension.startTastePreferencesActivity
import com.example.huetaste.common.navigation.SuggestionArgs
import com.example.huetaste.databinding.ActivitySuggestionReviewBinding
import com.example.huetaste.databinding.ItemProfileRowBinding
import com.example.huetaste.domain.model.DailyNeed
import com.example.huetaste.feature.taste_setup.icon
import com.example.huetaste.feature.taste_setup.label
import com.example.huetaste.feature.taste_setup.render
import com.example.huetaste.feature.taste_setup.shortLabel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

/** Between Home's suggest button and the results: the user checks, and can edit, what the suggestion is based on. */
@AndroidEntryPoint
class SuggestionReviewActivity : LsActivity<ActivitySuggestionReviewBinding>(ActivitySuggestionReviewBinding::inflate) {
    private val viewModel: SuggestionReviewViewModel by viewModels()

    private val moneyFormat = NumberFormat.getIntegerInstance(Locale("vi", "VN"))

    override fun viewCreated() {
        bindRows()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun bindRows() = with(binding) {
        bindRow(rowTaste, R.drawable.ic_restaurant, R.color.hue_primary_fixed, R.color.hue_primary, R.string.profile_taste_title)
        bindRow(rowDiet, R.drawable.ic_eco, R.color.hue_secondary_fixed, R.color.hue_on_secondary_fixed, R.string.profile_diet_title)
        bindRow(rowNeed, R.drawable.ic_tune, R.color.hue_tertiary_fixed, R.color.hue_tertiary, R.string.profile_need_title)

        backButton.clicks { handleBackPressed() }
        status.root.clicks { viewModel.load() }
        rowTaste.root.clicks { startTastePreferencesActivity() }
        rowDiet.root.clicks { startDietPreferencesActivity() }
        rowNeed.root.clicks { startDailyNeedsActivity() }
        startButton.clicks { startContextSuggestionActivity(viewModel.uiState.value.args) }
    }

    /** Back from an editor (or from the background): the saved data may have changed. Rotation does not come here. */
    override fun onRestart() {
        super.onRestart()
        viewModel.load()
    }

    private fun render(state: SuggestionReviewUiState) = with(binding) {
        showArgs(state.args)
        status.render(state.status, isEmpty = false)
        val profile = state.profile
        profileCard.isVisible = profile != null
        if (profile == null) return@with

        rowTaste.rowSubtitle.text = shortList(profile.tastes) ?: getString(R.string.taste_summary_none)
        rowDiet.rowSubtitle.text = profile.diets.joinToString(", ").ifEmpty { getString(R.string.taste_diet_normal) }
        rowNeed.rowSubtitle.text = needsSummary(profile.needs)
    }

    private fun showArgs(args: SuggestionArgs) = with(binding) {
        val meal = args.meal
        mealChip.isVisible = meal != null
        if (meal != null) {
            mealIcon.setImageResource(meal.icon)
            mealText.setText(meal.label)
        }
        val budget = args.budget
        budgetChip.isVisible = budget != null
        if (budget != null) budgetText.text = getString(R.string.review_budget_max, moneyFormat.format(budget))
        // Home only passes a note; the meal and budget are today's needs, listed below.
        thisTimeCard.isVisible = meal != null || budget != null || !args.note.isNullOrBlank()
        noteText.isVisible = !args.note.isNullOrBlank()
        noteText.text = args.note?.let { getString(R.string.review_note, it) }
    }

    /** "Cay, Ngọt, Món nước +2"; null when nothing is chosen. */
    private fun shortList(names: List<String>): String? {
        if (names.isEmpty()) return null
        val shown = names.take(SHOWN_NAMES).joinToString(", ")
        val rest = names.size - SHOWN_NAMES
        return if (rest > 0) getString(R.string.review_more, shown, rest) else shown
    }

    /** "Trưa, Tối · 30k – 60k · Tránh 2 nguyên liệu". The budget, note and ingredients are the same for every meal. */
    private fun needsSummary(needs: List<DailyNeed>): String {
        if (needs.isEmpty()) return getString(R.string.review_needs_none)
        val first = needs.first()
        return listOfNotNull(
            needs.joinToString(", ") { getString(it.meal.shortLabel) },
            first.budget?.let { getString(it.label) },
            first.avoidIngredientIds.size.takeIf { it > 0 }?.let { getString(R.string.review_needs_avoid, it) },
        ).joinToString(" · ")
    }

    private fun bindRow(
        row: ItemProfileRowBinding,
        @DrawableRes icon: Int,
        @ColorRes iconBackground: Int,
        @ColorRes iconTint: Int,
        @StringRes title: Int,
    ) {
        row.rowIcon.setImageResource(icon)
        row.rowIcon.backgroundTintList = ContextCompat.getColorStateList(this, iconBackground)
        row.rowIcon.imageTintList = ContextCompat.getColorStateList(this, iconTint)
        row.rowTitle.setText(title)
        row.rowSubtitle.isVisible = true
    }

    companion object {
        private const val SHOWN_NAMES = 3

        fun newIntent(context: Context, args: SuggestionArgs): Intent =
            args.putInto(Intent(context, SuggestionReviewActivity::class.java))
    }
}
