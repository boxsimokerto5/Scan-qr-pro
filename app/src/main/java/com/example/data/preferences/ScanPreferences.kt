package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class SupportedLanguage(
    val code: String, // "SYSTEM", "en", "in", "es", "pt", "fr", "de", "ar", "zh", "ja", "hi"
    val displayName: String,
    val nativeName: String
)

class ScanPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("scanqrpro_settings", Context.MODE_PRIVATE)

    private val _autoCopy = MutableStateFlow(prefs.getBoolean(KEY_AUTO_COPY, true))
    val autoCopy: StateFlow<Boolean> = _autoCopy.asStateFlow()

    private val _vibration = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION, true))
    val vibration: StateFlow<Boolean> = _vibration.asStateFlow()

    private val _beepSound = MutableStateFlow(prefs.getBoolean(KEY_BEEP, true))
    val beepSound: StateFlow<Boolean> = _beepSound.asStateFlow()

    private val savedTheme = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
    private val _themeMode = MutableStateFlow(
        try { AppThemeMode.valueOf(savedTheme) } catch (_: Exception) { AppThemeMode.SYSTEM }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val savedLang = prefs.getString(KEY_APP_LANGUAGE, "SYSTEM") ?: "SYSTEM"
    private val _appLanguage = MutableStateFlow(savedLang)
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    fun setAutoCopy(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_COPY, enabled).apply()
        _autoCopy.value = enabled
    }

    fun setVibration(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION, enabled).apply()
        _vibration.value = enabled
    }

    fun setBeepSound(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BEEP, enabled).apply()
        _beepSound.value = enabled
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setAppLanguage(langCode: String) {
        prefs.edit().putString(KEY_APP_LANGUAGE, langCode).apply()
        _appLanguage.value = langCode
    }

    fun toggleLightDark(isCurrentlyDark: Boolean) {
        val nextMode = if (isCurrentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(nextMode)
    }

    companion object {
        private const val KEY_AUTO_COPY = "key_auto_copy"
        private const val KEY_VIBRATION = "key_vibration"
        private const val KEY_BEEP = "key_beep"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_APP_LANGUAGE = "key_app_language"

        val AVAILABLE_LANGUAGES = listOf(
            SupportedLanguage("SYSTEM", "Auto (Device System)", "Otomatis / System"),
            SupportedLanguage("en", "English", "English (Global)"),
            SupportedLanguage("in", "Indonesian", "Bahasa Indonesia"),
            SupportedLanguage("es", "Spanish", "Español"),
            SupportedLanguage("pt", "Portuguese", "Português"),
            SupportedLanguage("fr", "French", "Français"),
            SupportedLanguage("de", "German", "Deutsch"),
            SupportedLanguage("ar", "Arabic", "العربية"),
            SupportedLanguage("zh", "Chinese", "中文 (简体)"),
            SupportedLanguage("ja", "Japanese", "日本語"),
            SupportedLanguage("hi", "Hindi", "हिन्दी")
        )
    }
}
