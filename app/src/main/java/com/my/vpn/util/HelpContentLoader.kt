package com.my.vpn.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.my.vpn.data.model.AppLanguage
import com.my.vpn.data.local.AppSettingsStorage

data class HelpSection(
    val number: Int,
    val title: String,
    val paragraphs: List<String>
)

object HelpContentLoader {

    private val gson = Gson()
    private val listType = object : TypeToken<List<HelpSection>>() {}.type

    fun load(context: Context): List<HelpSection> {
        val english = AppSettingsStorage.readLanguageBlocking(context) == AppLanguage.ENGLISH
        val assetName = if (english) "help_en.json" else "help_ru.json"
        return loadAsset(context, assetName)
    }

    private fun loadAsset(context: Context, assetName: String): List<HelpSection> =
        runCatching {
            context.assets.open(assetName).bufferedReader().use { reader ->
                gson.fromJson<List<HelpSection>>(reader, listType)
            }
        }.getOrDefault(emptyList())
}
