package com.documind.app.data.preferences

import android.content.Context
import android.content.SharedPreferences

class OnboardingPreferences(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun isOnboardingCompleted(): Boolean {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted() {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    companion object {
        private const val PREFERENCES_NAME: String = "documind_prefs"
        private const val KEY_ONBOARDING_COMPLETED: String = "onboarding_completed"
    }
}
