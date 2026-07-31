package com.my.vpn.util

import android.content.Context
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Разбор ссылок Happ (happ://add/…, happ://crypt4|5/…).
 * @see https://www.happ.su/main/dev-docs/crypto-link
 */
object HappLinkResolver {

    private val HAPP_ADD_PREFIX = Regex("^happ://add/", RegexOption.IGNORE_CASE)
    private val HAPP_CRYPTO_PREFIX = Regex("^happ://crypt[45]/", RegexOption.IGNORE_CASE)

    sealed class Result {
        /** HTTP(S) URL подписки для загрузки. */
        data class SubscriptionUrl(val url: String) : Result()

        /** Текст подписки или список share-ссылок. */
        data class RawContent(val text: String) : Result()

        /** Зашифрованная подписка Happ — без ключей приложения не расшифровать. */
        data class Encrypted(val link: String) : Result()

        data class Error(val message: String) : Result()
    }

    fun resolve(input: String, context: Context? = null): Result {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Result.Error("Пустая ссылка")

        when {
            HAPP_CRYPTO_PREFIX.containsMatchIn(trimmed) -> {
                val decrypted = HappCryptDecoder.decrypt(trimmed, context)
                if (decrypted != null) {
                    return when {
                        decrypted.startsWith("http://", true) ||
                            decrypted.startsWith("https://", true) ->
                            Result.SubscriptionUrl(decrypted.trim())
                        decrypted.contains("://") -> Result.RawContent(decrypted)
                        else -> Result.RawContent(decrypted)
                    }
                }
                return Result.Encrypted(trimmed)
            }

            HAPP_ADD_PREFIX.containsMatchIn(trimmed) -> {
                val rest = trimmed.replace(HAPP_ADD_PREFIX, "")
                val url = URLDecoder.decode(rest, StandardCharsets.UTF_8).trim()
                return if (url.startsWith("http://", true) || url.startsWith("https://", true)) {
                    Result.SubscriptionUrl(url)
                } else {
                    Result.Error("Некорректный URL в happ://add/")
                }
            }

            trimmed.startsWith("happ://", ignoreCase = true) ->
                return Result.Error(
                    "Формат Happ не поддерживается. Используйте happ://add/https://… или обычный URL подписки."
                )
        }

        if (trimmed.contains("://")) return Result.RawContent(trimmed)
        return Result.RawContent(trimmed)
    }

    /** Нормализует ввод подписки (URL, файл, QR, Happ). */
    fun normalizeSubscriptionInput(raw: String, context: Context? = null): String {
        return when (val r = resolve(raw, context)) {
            is Result.SubscriptionUrl -> r.url
            is Result.RawContent -> r.text
            is Result.Encrypted -> raw
            is Result.Error -> raw
        }
    }

    fun isEncryptedHappLink(input: String): Boolean =
        HAPP_CRYPTO_PREFIX.containsMatchIn(input.trim())
}
