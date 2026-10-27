package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_reader_prefs")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class FitMode {
    FIT_WIDTH, FIT_PAGE, ACTUAL_SIZE
}

enum class ScrollDirection {
    VERTICAL_CONTINUOUS, SINGLE_PAGE
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fitMode: FitMode = FitMode.FIT_WIDTH,
    val scrollDirection: ScrollDirection = ScrollDirection.VERTICAL_CONTINUOUS,
    val keepScreenOn: Boolean = true,
    val autoHideBars: Boolean = false
)

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FIT_MODE = stringPreferencesKey("fit_mode")
        val SCROLL_DIRECTION = stringPreferencesKey("scroll_direction")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val AUTO_HIDE_BARS = booleanPreferencesKey("auto_hide_bars")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        val themeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        val fitString = preferences[PreferencesKeys.FIT_MODE] ?: FitMode.FIT_WIDTH.name
        val scrollString = preferences[PreferencesKeys.SCROLL_DIRECTION] ?: ScrollDirection.VERTICAL_CONTINUOUS.name
        val keepScreenOn = preferences[PreferencesKeys.KEEP_SCREEN_ON] ?: true
        val autoHideBars = preferences[PreferencesKeys.AUTO_HIDE_BARS] ?: false

        UserPreferences(
            themeMode = runCatching { ThemeMode.valueOf(themeString) }.getOrDefault(ThemeMode.SYSTEM),
            fitMode = runCatching { FitMode.valueOf(fitString) }.getOrDefault(FitMode.FIT_WIDTH),
            scrollDirection = runCatching { ScrollDirection.valueOf(scrollString) }.getOrDefault(ScrollDirection.VERTICAL_CONTINUOUS),
            keepScreenOn = keepScreenOn,
            autoHideBars = autoHideBars
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setFitMode(mode: FitMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIT_MODE] = mode.name
        }
    }

    suspend fun setScrollDirection(direction: ScrollDirection) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCROLL_DIRECTION] = direction.name
        }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun setAutoHideBars(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_HIDE_BARS] = enabled
        }
    }
}
