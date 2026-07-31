package com.my.vpn.util

import android.app.Application
import androidx.annotation.StringRes

/** Локализованные строки для ViewModel (после [LocaleHelper.applyLanguage]). */
class AppStrings(private val app: Application) {

    fun get(@StringRes resId: Int, vararg formatArgs: Any): String =
        app.getString(resId, *formatArgs)
}
