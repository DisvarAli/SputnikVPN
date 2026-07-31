package com.my.vpn.update

import android.content.Context

/** Локальный журнал из assets/CHANGELOG.md — блок «Что нового» в меню без интернета. */
object BundledChangelog {

    fun load(context: Context): String =
        runCatching {
            context.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() }
        }.getOrElse { "См. справку в приложении (Помощь)." }
}
