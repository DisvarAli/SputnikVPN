package com.my.vpn.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.my.vpn.data.local.AppSettingsStorage
import com.my.vpn.data.model.AppLanguage
import java.util.Locale

object LocaleHelper {

    fun wrap(context: Context, language: AppLanguage): Context {
        val locale = localeFor(language) ?: return context
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }

    /**
     * Меняет локаль всего приложения (включая [android.app.Application] и ViewModel).
     * После вызова нужен [android.app.Activity.recreate] на API &lt; 33.
     */
    fun applyLanguage(context: Context, language: AppLanguage) {
        AppSettingsStorage.writeLanguageEarly(context, language)
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.RUSSIAN -> LocaleListCompat.forLanguageTags("ru")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
        localeFor(language)?.let { Locale.setDefault(it) }
    }

    fun readLanguage(context: Context): AppLanguage =
        AppSettingsStorage.readLanguageBlocking(context)

    private fun localeFor(language: AppLanguage): Locale? = when (language) {
        AppLanguage.SYSTEM -> null
        AppLanguage.RUSSIAN -> Locale.forLanguageTag("ru")
        AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
    }
}
