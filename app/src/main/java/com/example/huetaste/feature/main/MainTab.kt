package com.example.huetaste.feature.main

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.example.huetaste.R
import com.example.huetaste.feature.ai_assistant.AiAssistantFragment
import com.example.huetaste.feature.favorites.FavoritesFragment
import com.example.huetaste.feature.home.HomeFragment
import com.example.huetaste.feature.profile.ProfileFragment
import com.example.huetaste.feature.search.SearchFragment

enum class MainTab(@IdRes val menuItemId: Int) {
    HOME(R.id.nav_home),
    SEARCH(R.id.nav_search),
    AI_ASSISTANT(R.id.nav_ai_assistant),
    FAVORITES(R.id.nav_favorites),
    PROFILE(R.id.nav_profile);

    fun createFragment(): Fragment = when (this) {
        HOME -> HomeFragment()
        SEARCH -> SearchFragment()
        AI_ASSISTANT -> AiAssistantFragment()
        FAVORITES -> FavoritesFragment()
        PROFILE -> ProfileFragment()
    }
}
