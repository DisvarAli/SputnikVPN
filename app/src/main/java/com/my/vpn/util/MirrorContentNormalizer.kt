package com.my.vpn.util

/**
 * Нормализация тела ответа зеркала: сырой TXT или HTML-обёртка (Яндекс Переводчик).
 */
object MirrorContentNormalizer {

    private val SHARE_LINK = Regex(
        "(?i)((?:vless|vmess|trojan|ss|hysteria2|hy2|tuic|wireguard|wg)://[^\\s\"'<>]+)"
    )
    fun normalize(body: String): String {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return trimmed
        // Уже похоже на подписку / base64 / JSON
        if (!looksLikeHtml(trimmed)) return trimmed
        val extracted = extractLinksFromHtml(trimmed)
        return extracted.ifBlank { trimmed }
    }

    private fun looksLikeHtml(body: String): Boolean {
        val head = body.take(400).lowercase()
        return head.contains("<html") || head.contains("<!doctype") ||
            head.contains("<body") || head.contains("translate.yandex")
    }

    private fun extractLinksFromHtml(html: String): String {
        val links = LinkedHashSet<String>()
        SHARE_LINK.findAll(html).forEach { m ->
            links.add(decodeHtmlEntities(m.groupValues[1].trimEnd(',', ';', '.', ')')))
        }
        val preBlocks = Regex("(?is)<pre[^>]*>(.*?)</pre>").findAll(html)
        preBlocks.forEach { block ->
            block.groupValues[1].lineSequence().forEach { line ->
                val t = decodeHtmlEntities(line.trim())
                if (t.contains("://")) {
                    links.add(t)
                }
            }
        }
        return links.joinToString("\n")
    }

    private fun decodeHtmlEntities(s: String): String =
        s.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
}
