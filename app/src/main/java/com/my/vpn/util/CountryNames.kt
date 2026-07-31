package com.my.vpn.util

import com.my.vpn.data.model.AppLanguage
import java.util.Locale

object CountryNames {

     private val FLAG_PREFIX = Regex("^([\\uD83C][\\uDDE6-\\uDDFF][\\uD83C][\\uDDE6-\\uDDFF])\\s*")

  private val EN_TO_RU = mapOf(
    "Netherlands" to "Нидерланды",
    "Holland" to "Нидерланды",
    "Germany" to "Германия",
    "France" to "Франция",
    "United Kingdom" to "Великобритания",
    "UK" to "Великобритания",
    "England" to "Англия",
    "USA" to "США",
    "United States" to "США",
    "US" to "США",
    "Canada" to "Канада",
    "Finland" to "Финляндия",
    "Sweden" to "Швеция",
    "Norway" to "Норвегия",
    "Denmark" to "Дания",
    "Poland" to "Польша",
    "Czech Republic" to "Чехия",
    "Czechia" to "Чехия",
    "Austria" to "Австрия",
    "Switzerland" to "Швейцария",
    "Italy" to "Италия",
    "Spain" to "Испания",
    "Portugal" to "Португалия",
    "Belgium" to "Бельгия",
    "Luxembourg" to "Люксембург",
    "Ireland" to "Ирландия",
    "Romania" to "Румыния",
    "Bulgaria" to "Болгария",
    "Hungary" to "Венгрия",
    "Greece" to "Греция",
    "Turkey" to "Турция",
    "Israel" to "Израиль",
    "UAE" to "ОАЭ",
    "United Arab Emirates" to "ОАЭ",
    "Singapore" to "Сингапур",
    "Japan" to "Япония",
    "South Korea" to "Южная Корея",
    "Korea" to "Корея",
    "Hong Kong" to "Гонконг",
    "Taiwan" to "Тайвань",
    "India" to "Индия",
    "Australia" to "Австралия",
    "New Zealand" to "Новая Зеландия",
    "Brazil" to "Бразилия",
    "Argentina" to "Аргентина",
    "Mexico" to "Мексика",
    "Chile" to "Чили",
    "Colombia" to "Колумбия",
    "South Africa" to "ЮАР",
    "Egypt" to "Египет",
    "Ukraine" to "Украина",
    "Belarus" to "Беларусь",
    "Kazakhstan" to "Казахстан",
    "Latvia" to "Латвия",
    "Lithuania" to "Литва",
    "Estonia" to "Эстония",
    "Moldova" to "Молдова",
    "Serbia" to "Сербия",
    "Croatia" to "Хорватия",
    "Slovakia" to "Словакия",
    "Slovenia" to "Словения",
    "Iceland" to "Исландия",
    "Russia" to "Россия",
    "Russian Federation" to "Россия",
    "China" to "Китай",
    "Thailand" to "Таиланд",
    "Vietnam" to "Вьетнам",
    "Indonesia" to "Индонезия",
    "Malaysia" to "Малайзия",
    "Philippines" to "Филиппины",
    "Pakistan" to "Пакистан",
    "Iran" to "Иран",
    "Iraq" to "Ирак",
    "Qatar" to "Катар",
    "Saudi Arabia" to "Саудовская Аравия",
    "Armenia" to "Армения",
    "Georgia" to "Грузия",
    "Azerbaijan" to "Азербайджан",
    "Cyprus" to "Кипр",
    "Malta" to "Мальта",
    "Monaco" to "Монако",
    "Other" to "Другое",
    "Другое" to "Другое"
  )

  fun toDisplay(raw: String, language: AppLanguage): String {
    val useRussian = when (language) {
      AppLanguage.RUSSIAN -> true
      AppLanguage.ENGLISH -> false
      AppLanguage.SYSTEM -> Locale.getDefault().language.startsWith("ru")
    }
    return if (useRussian) toRussian(raw) else toLatinDisplay(raw)
  }

  private fun toLatinDisplay(raw: String): String {
    if (raw.isBlank()) return "Other"
    val flag = FLAG_PREFIX.find(raw)?.groupValues?.get(1).orEmpty()
    val namePart = FLAG_PREFIX.replace(raw, "").trim()
      .split(",", "|", "-", "·")
      .firstOrNull()
      ?.trim()
      .orEmpty()
      .ifBlank { raw.trim() }
    return if (flag.isNotBlank()) "$flag $namePart" else namePart
  }

  fun toRussian(raw: String): String {
    if (raw.isBlank()) return "Другое"
    val flag = FLAG_PREFIX.find(raw)?.groupValues?.get(1).orEmpty()
    val namePart = FLAG_PREFIX.replace(raw, "").trim()
      .split(",", "|", "-", "·")
      .firstOrNull()
      ?.trim()
      .orEmpty()
      .ifBlank { raw.trim() }

  val translated = translateToken(namePart)
    return if (flag.isNotBlank()) "$flag $translated" else translated
  }

  private fun translateToken(name: String): String {
    EN_TO_RU[name]?.let { return it }
    EN_TO_RU.entries.firstOrNull { (en, _) ->
      name.equals(en, ignoreCase = true) ||
        name.startsWith("$en ", ignoreCase = true) ||
        name.contains(" $en", ignoreCase = true)
    }?.value?.let { return it }
    if (name.any { it.code in 0x0400..0x04FF }) return name
    return name
  }
}
