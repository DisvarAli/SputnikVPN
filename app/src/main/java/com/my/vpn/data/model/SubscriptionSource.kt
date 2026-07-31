package com.my.vpn.data.model

import com.my.vpn.AppConstants
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class SubscriptionListType(
    val fileName: String,
    val title: String,
    val description: String,
    /** Встроенные списки не показываем в «Подписках» — только свои URL/файлы. */
    val visibleInUi: Boolean = false,
    val fetchFromRemote: Boolean = true
) {
    BLACK_MOBILE(
        fileName = "BLACK_VLESS_RUS_mobile.txt",
        title = "Чёрный · mobile",
        description = "ТОП-150 для телефона (микс протоколов)"
    ),
    BLACK_VLESS_FULL(
        fileName = "BLACK_VLESS_RUS.txt",
        title = "Чёрный · VLESS",
        description = "Полный чёрный список VLESS"
    ),
    BLACK_SS_ALL(
        fileName = "BLACK_SS+All_RUS.txt",
        title = "Чёрный · SS",
        description = "Shadowsocks, полный чёрный список"
    ),
    WHITE_MOBILE_1(
        fileName = "Vless-Reality-White-Lists-Rus-Mobile.txt",
        title = "Белый · Reality #1",
        description = "VLESS Reality, первые 150 (режим БС)"
    ),
    WHITE_MOBILE_2(
        fileName = "Vless-Reality-White-Lists-Rus-Mobile-2.txt",
        title = "Белый · Reality #2",
        description = "VLESS Reality, следующие 150"
    ),
    WHITE_SNI_ALL(
        fileName = "WHITE-SNI-RU-all.txt",
        title = "Белый · SNI",
        description = "Конфиги под белые SNI"
    ),
    WHITE_CIDR_ALL(
        fileName = "WHITE-CIDR-RU-all.txt",
        title = "Белый · CIDR",
        description = "Полный белый CIDR"
    ),
    WHITE_CIDR_CHECKED(
        fileName = "WHITE-CIDR-RU-checked.txt",
        title = "Белый · CIDR ✓",
        description = "Проверенные белые CIDR"
    );

    val gridLabel: String get() = title

    companion object {
        val vpnSources: List<SubscriptionListType> = entries.toList()

        val remoteSources: List<SubscriptionListType> =
            entries.filter { it.fetchFromRemote }

        val uiSources: List<SubscriptionListType> =
            entries.filter { it.visibleInUi }
    }
}

enum class MirrorType(val label: String) {
    GITHACK("GitHack"),
    GITLAB("GitLab"),
    CODEBERG("Codeberg"),
    GITEA("Gitea"),
    GITHUB("GitHub"),
    GITHUB_LEGACY("GitHub raw"),
    BITBUCKET("Bitbucket"),
    SOURCEHUT("SourceHut"),
    JSDELIVR("jsDelivr"),
    YANDEX_TRANSLATE("Яндекс Переводчик")
}

object SubscriptionUrls {

    private const val REPO = AppConstants.CONFIG_INSPIRATION_REPO

    fun buildUrl(mirror: MirrorType, fileName: String): String {
        val path = if (fileName.contains("/")) {
            fileName.split("/").joinToString("/") { encodePathSegment(it) }
        } else {
            encodePathSegment(fileName)
        }
        return when (mirror) {
            MirrorType.GITHACK ->
                "https://raw.githack.com/$REPO/main/$path"
            MirrorType.GITLAB ->
                "https://gitlab.com/$REPO/-/raw/main/$path"
            MirrorType.CODEBERG ->
                "https://codeberg.org/$REPO/raw/branch/main/$path"
            MirrorType.GITEA ->
                "https://gitea.com/$REPO/raw/branch/main/$path"
            MirrorType.GITHUB ->
                "https://raw.githubusercontent.com/$REPO/refs/heads/main/$path"
            MirrorType.GITHUB_LEGACY ->
                "https://raw.githubusercontent.com/$REPO/main/$path"
            MirrorType.BITBUCKET ->
                "https://bitbucket.org/$REPO/raw/main/$path"
            MirrorType.SOURCEHUT ->
                "https://git.sr.ht/~igareck/vpn-configs-for-russia/raw/main/$path"
            MirrorType.JSDELIVR ->
                "https://cdn.jsdelivr.net/gh/$REPO@main/$path"
            MirrorType.YANDEX_TRANSLATE -> {
                // Режим белых списков: Yandex Translate + Bitbucket RAW
                // (рекомендация igareck/vpn-configs-for-russia; GitHub-RAW часто режется).
                val raw = "https://bitbucket.org/$REPO/raw/main/$path"
                val encoded = URLEncoder.encode(raw, StandardCharsets.UTF_8.name())
                "https://translate.yandex.ru/translate?url=$encoded&lang=de-de"
            }
        }
    }

    private fun encodePathSegment(segment: String): String {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8.name())
            .replace("+", "%2B")
    }

    /**
     * Порядок зеркал при блокировке GitHub / raw.githubusercontent.com
     * (по [igareck/vpn-configs-for-russia](https://github.com/igareck/vpn-configs-for-russia)):
     * GitLab → GitHack → FOSS-зеркала → Bitbucket → Яндекс+Bitbucket (БС) → GitHub.
     * jsDelivr не используем: CDN кэшируется с большим запозданием.
     */
    val defaultMirrorOrder = listOf(
        MirrorType.GITLAB,
        MirrorType.GITHACK,
        MirrorType.CODEBERG,
        MirrorType.GITEA,
        MirrorType.SOURCEHUT,
        MirrorType.BITBUCKET,
        MirrorType.YANDEX_TRANSLATE,
        MirrorType.GITHUB_LEGACY,
        MirrorType.GITHUB
    )

    const val CONFIG_REPO_URL = AppConstants.CONFIG_INSPIRATION_URL
}
