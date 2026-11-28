package com.example.huetaste.feature.language

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.isVisible
import com.base.common.base.LsActivity
import com.base.common.extension.clicks
import com.base.data.Prefs
import com.example.huetaste.databinding.ActivityLanguageBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Picks the app language, as in the newplantiap project: the choice is saved in [Prefs.language] and applied as the
 * app locale. Only Vietnamese texts exist for now, so the screens keep showing Vietnamese whatever is picked.
 */
@AndroidEntryPoint
class LanguageActivity : LsActivity<ActivityLanguageBinding>(ActivityLanguageBinding::inflate) {

    private val adapter = LanguageAdapter(::select)

    private lateinit var saved: AppLanguage

    /** Read before [viewCreated], which runs inside LsActivity.onCreate. */
    private var restored: AppLanguage? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        restored = savedInstanceState?.getString(KEY_SELECTED)?.let(AppLanguage::valueOf)
        super.onCreate(savedInstanceState)
    }

    override fun viewCreated() {
        saved = prefs.savedLanguage()
        binding.languageList.adapter = adapter
        adapter.data = AppLanguage.entries
        adapter.selected = restored ?: saved
        binding.confirmButton.isVisible = adapter.selected != saved

        binding.backButton.clicks { handleBackPressed() }
        binding.confirmButton.clicks { apply(adapter.selected) }
    }

    private fun select(language: AppLanguage) {
        adapter.selected = language
        binding.confirmButton.isVisible = language != saved
    }

    private fun apply(language: AppLanguage) {
        prefs.language.set(language.languageCode)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.languageCode))
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED, adapter.selected.name)
    }

    companion object {
        private const val KEY_SELECTED = "language_selected"

        fun newIntent(context: Context): Intent = Intent(context, LanguageActivity::class.java)
    }
}

/** [Prefs.language] defaults to "en" in the base module, but this app is Vietnamese until the user picks a language. */
fun Prefs.savedLanguage(): AppLanguage =
    if (language.isSet) AppLanguage.fromCode(language.get()) else AppLanguage.VIETNAMESE
