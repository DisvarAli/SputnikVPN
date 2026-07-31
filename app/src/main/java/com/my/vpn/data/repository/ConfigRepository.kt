package com.my.vpn.data.repository

import android.content.Context
import com.my.vpn.AppConstants
import com.my.vpn.data.local.AppSettingsStorage
import com.my.vpn.data.local.ConfigStorage
import com.my.vpn.data.local.CustomSubscriptionStorage
import com.my.vpn.data.model.CountryGroup
import com.my.vpn.data.model.CustomSubscription
import com.my.vpn.data.model.MirrorType
import com.my.vpn.data.model.ConfigTestMode
import com.my.vpn.data.model.SubscriptionListType
import com.my.vpn.data.model.SubscriptionTraffic
import com.my.vpn.data.model.SubscriptionUrls
import com.my.vpn.data.model.ConnectionBypassProfile
import com.my.vpn.data.model.VpnConfig
import com.my.vpn.data.parser.SubscriptionParser
import com.my.vpn.util.ConfigFetchBypass
import com.my.vpn.util.ConfigFilters
import com.my.vpn.util.HappLinkResolver
import com.my.vpn.util.SubscriptionTrafficParser
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.TimeUnit

class ConfigRepository(
    private val storage: ConfigStorage,
    private val customStorage: CustomSubscriptionStorage,
    private val appSettings: AppSettingsStorage,
    private val tester: ConfigTester,
    private val appContext: Context,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    private var lastUsedMirror: MirrorType? = null

    suspend fun loadCustomSubscriptions(): List<CustomSubscription> = customStorage.loadAll()

    suspend fun addCustomSubscription(title: String, url: String): CustomSubscription {
        val resolvedUrl = when (val r = HappLinkResolver.resolve(url.trim(), appContext)) {
            is HappLinkResolver.Result.SubscriptionUrl -> r.url
            is HappLinkResolver.Result.Encrypted ->
                error("Не удалось расшифровать Happ-ссылку (crypt). Проверьте формат или добавьте обычный https:// URL.")
            is HappLinkResolver.Result.Error -> error(r.message)
            is HappLinkResolver.Result.RawContent ->
                if (r.text.startsWith("http", ignoreCase = true)) r.text else error("Укажите http(s) URL подписки")
        }
        val sub = CustomSubscription(title = title.trim(), url = resolvedUrl)
        customStorage.add(sub)
        return sub
    }

    suspend fun addCustomSubscriptionFromContent(title: String, content: String): CustomSubscription {
        val trimmed = HappLinkResolver.normalizeSubscriptionInput(content, appContext)
        if (HappLinkResolver.isEncryptedHappLink(content) && !trimmed.contains("://")) {
            error("Не удалось расшифровать Happ-ссылку (crypt). Добавьте happ://add/https://… или файл с конфигами.")
        }
        require(trimmed.contains("://") || trimmed.lines().any { it.contains("://") }) {
            "В файле нет ссылок на конфиги"
        }
        val sub = CustomSubscription(
            title = title.trim(),
            url = "inline://import/${System.currentTimeMillis()}",
            inlineContent = trimmed
        )
        customStorage.add(sub)
        return sub
    }

    suspend fun removeCustomSubscription(id: String) {
        customStorage.remove(id)
        val remaining = storage.loadConfigs().filter { it.customSourceId != id }
        storage.saveConfigs(
            remaining,
            storage.getContentHash().orEmpty(),
            buildStableSourceStats(remaining),
            storage.loadBuiltinTraffic()
        )
    }

    /** Весь кэш без фильтра по пингу */
    suspend fun loadCachedConfigs(): List<VpnConfig> =
        storage.loadConfigs().filter { ConfigFilters.isInSubscriptionList(it) }

    suspend fun loadDisplayConfigs(allowPendingPing: Boolean = false): List<VpnConfig> =
        ConfigFilters.filterForDisplay(loadCachedConfigs(), allowPendingPing)
            .sortedBy { it.pingMs ?: Long.MAX_VALUE }

    suspend fun loadBuiltinTraffic(): Map<SubscriptionListType, SubscriptionTraffic> =
        storage.loadBuiltinTraffic()

    suspend fun isConfigReachable(config: VpnConfig): Boolean =
        tester.isReachable(config, ConfigTestMode.TCP_ONLY)

    /** Быстрый TCP-пинг выборки (после загрузки подписок, без VPN-теста). */
    suspend fun repingCachedConfigsFast(
        onProgress: ((String) -> Unit)? = null,
        pingScope: ConfigPingScope? = null
    ): RefreshResult =
        refreshPingsInternal(
            onProgress = onProgress,
            fastSample = true,
            forceMode = ConfigTestMode.TCP_ONLY,
            pingScope = pingScope
        )

    /** Только для ручной проверки в меню — без ложных срабатываний */
    suspend fun peekSubscriptionChangesOnRemote(): Boolean = withContext(Dispatchers.IO) {
        val local = storage.getContentHash() ?: return@withContext false
        val bundle = fetchAllSubscriptions()
        if (bundle.pairs.size < SubscriptionListType.remoteSources.size) return@withContext false
        val remote = bundle.combinedHash ?: return@withContext false
        remote != local
    }

    suspend fun acknowledgeCurrentSubscriptionCache() {
        storage.getContentHash()?.let { appSettings.setAcknowledgedSubscriptionHash(it) }
    }

    /** Полная загрузка с GitHub + кэш (только по кнопке обновления в меню). */
    suspend fun fullRefreshFromRemote(
        onProgress: ((String) -> Unit)? = null,
        deferPing: Boolean = true
    ): RefreshResult = withContext(Dispatchers.IO) {
        val parsedResult = fetchAndParseAll(onProgress)
        if (parsedResult is RefreshResult.Error) return@withContext parsedResult
        val parsed = parsedResult as RefreshResult.Parsed
        saveFullCache(parsed.configs, parsed.contentHash, parsed.builtinTraffic, onProgress)
        if (deferPing) {
            appSettings.setFullCachePingScheduled(true)
            buildDeferredSuccess(parsed)
        } else {
            refreshPingsInternal(onProgress)
        }
    }

    /**
     * Если обычная загрузка не удалась — перебор зеркал и белых списков РФ, затем полный refresh.
     */
    suspend fun fullRefreshViaWhiteListBypass(
        onIndexedProgress: ((Int, Int, String) -> Unit)? = null,
        onProgress: ((String) -> Unit)? = null
    ): RefreshResult = withContext(Dispatchers.IO) {
        onProgress?.invoke("Перебор зеркал (белые списки РФ)…")
        val path = ConfigFetchBypass.findWorkingFetchPath { p ->
            onIndexedProgress?.invoke(p.index, p.total, p.label)
            onProgress?.invoke("${p.index}/${p.total} ${p.label}")
        } ?: return@withContext RefreshResult.Error(
            "Не удалось загрузить конфиги ни через одно зеркало белых списков. " +
                "Попробуйте позже или добавьте свою подписку."
        )
        storage.savePreferredMirror(path.mirror)
        lastUsedMirror = path.mirror
        onProgress?.invoke("Зеркало: ${path.mirror.label}. Загрузка всех списков…")
        fullRefreshFromRemote(onProgress)
    }

    /** Только пинг по уже загруженному кэшу */
    suspend fun repingCachedConfigs(onProgress: ((String) -> Unit)? = null): RefreshResult =
        repingInScope(scope = null, onProgress = onProgress)

    suspend fun repingInScope(
        scope: ConfigPingScope?,
        onProgress: ((String) -> Unit)? = null,
        fastSample: Boolean = false,
        forceMode: ConfigTestMode? = null
    ): RefreshResult = withContext(Dispatchers.IO) {
        val cache = loadCachedConfigs()
        if (cache.isEmpty()) {
            return@withContext RefreshResult.Error("Кэш пуст. Обновите подписки в меню.")
        }
        val scoped = configsForScope(cache, scope)
        if (scoped.isEmpty()) {
            return@withContext RefreshResult.Error("Нет серверов в выбранной подписке.")
        }
        refreshPingsInternal(
            onProgress = onProgress,
            bypassProfile = null,
            fastSample = fastSample,
            forceMode = forceMode,
            pingScope = scope
        )
    }

    /** Пинг с учётом активных обходов (SNI/TLS, DoH). */
    suspend fun repingCachedWithBypass(
        profile: ConnectionBypassProfile,
        onProgress: ((Int, Int, String) -> Unit)? = null
    ): RefreshResult = withContext(Dispatchers.IO) {
        val cache = loadCachedConfigs()
        if (cache.isEmpty()) {
            return@withContext RefreshResult.Error("Кэш пуст. Обновите подписки в меню.")
        }
        refreshPingsInternal(
            onProgress = { msg -> onProgress?.invoke(0, 0, msg) },
            bypassProfile = profile,
            indexedProgress = onProgress
        )
    }

    suspend fun refreshPings(onProgress: ((String) -> Unit)? = null): RefreshResult =
        repingCachedConfigs(onProgress)

    suspend fun refreshCustomSubscriptionFromRemote(
        sub: CustomSubscription,
        onProgress: ((String) -> Unit)? = null,
        deferPing: Boolean = true,
        pingScope: ConfigPingScope.Custom = ConfigPingScope.Custom(sub.id)
    ): RefreshResult = withContext(Dispatchers.IO) {
        onProgress?.invoke("Загрузка «${sub.title}»…")
        val bodyText = sub.inlineContent
            ?: downloadUrl(sub.url)?.text
        if (bodyText == null) {
            return@withContext RefreshResult.Error("Не удалось загрузить «${sub.title}»")
        }
        val fetched = RemoteContent(bodyText, sha256(bodyText), null, null)

        fetched.traffic?.let { customStorage.updateTraffic(sub.id, it) }

        val parsed = SubscriptionParser.parseSubscription(bodyText)
            .map { it.copy(customSourceId = sub.id) }

        if (parsed.isEmpty()) {
            return@withContext RefreshResult.Error("Подписка «${sub.title}» пуста")
        }

        val cache = loadCachedConfigs().filter { it.customSourceId != sub.id }
        val merged = deduplicate(cache + parsed).take(AppConstants.MAX_CACHE_CONFIGS)
        storage.saveConfigs(
            merged,
            storage.getContentHash().orEmpty(),
            buildStableSourceStats(merged),
            storage.loadBuiltinTraffic()
        )
        if (deferPing) {
            appSettings.setFullCachePingScheduled(true)
            buildDeferredSuccess(
                RefreshResult.Parsed(merged, storage.getContentHash().orEmpty(), storage.loadBuiltinTraffic())
            )
        } else {
            refreshPingsInternal(onProgress = onProgress, pingScope = pingScope)
        }
    }

    suspend fun initialLoad(onProgress: ((String) -> Unit)? = null): RefreshResult =
        withContext(Dispatchers.IO) {
            val cached = loadCachedConfigs()
            if (cached.isNotEmpty() && storage.getContentHash() != null) {
                return@withContext RefreshResult.Success(
                    configs = cached,
                    groups = groupByCountry(ConfigFilters.filterForDisplay(cached, false)),
                    mirrorUsed = lastUsedMirror,
                    totalInSubscription = cached.size,
                    loadedSources = storage.loadSourceStats().size
                        .takeIf { it > 0 } ?: SubscriptionListType.remoteSources.size,
                    sourceStats = storage.loadSourceStats(),
                    builtinTraffic = storage.loadBuiltinTraffic(),
                    pingPending = false
                )
            }
            fullRefreshFromRemote(onProgress)
        }

    fun groupByCountry(configs: List<VpnConfig>): List<CountryGroup> {
        return configs
            .groupBy { groupKeyForConfig(it) }
            .entries
            .sortedWith(
                compareBy<Map.Entry<String, List<VpnConfig>>>(
                    { countrySortRank(it.key) },
                    { it.value.minOfOrNull { cfg -> cfg.pingMs ?: Long.MAX_VALUE } ?: Long.MAX_VALUE }
                )
            )
            .map { (country, items) ->
                CountryGroup(country, items.sortedBy { it.pingMs ?: Long.MAX_VALUE })
            }
    }

    private fun groupKeyForConfig(config: VpnConfig): String =
        com.my.vpn.util.CountryNames.toRussian(config.country)

    private fun countrySortRank(country: String): Int = when {
        country.contains(com.my.vpn.util.BypassPresets.COUNTRY_ANYCAST_MARKER, ignoreCase = true) -> 1
        else -> 0
    }

    suspend fun getLastUpdatedAt(): Long = storage.getLastUpdatedAt()

    /**
     * Один проход фонового пинга: сначала TCP по чанку без пинга, затем (если в настройках не TCP-only)
     * VPN-тест только для лучших по TCP — без запуска сотен measureOutboundDelay подряд.
     */
    suspend fun backgroundPingChunk(): Boolean = withContext(Dispatchers.IO) {
        val cache = loadCachedConfigs()
        if (cache.isEmpty()) return@withContext false

        val fastOptions = ConfigTester.TestRunOptions(fast = true)
        var merged = cache

        val pendingTcp = cache.filter { it.pingMs == null }
            .take(AppConstants.CONFIG_PING_WORK_CHUNK)
        if (pendingTcp.isNotEmpty()) {
            val tcpTested = tester.testConfigs(
                pendingTcp,
                ConfigTestMode.TCP_ONLY,
                bypass = null,
                onProgress = null,
                options = fastOptions
            )
            merged = mergePingResults(merged, tcpTested)
        }

        val userMode = appSettings.getConfigTestMode()
        if (userMode != ConfigTestMode.TCP_ONLY) {
            val proxyCandidates = merged
                .filter {
                    it.connectSupported &&
                        it.isAvailable &&
                        !it.proxyVerified
                }
                .sortedBy { it.pingMs ?: Long.MAX_VALUE }
                .take(AppConstants.CONFIG_PING_PROXY_CHUNK)
            if (proxyCandidates.isNotEmpty()) {
                val proxyTested = tester.testConfigs(
                    proxyCandidates,
                    ConfigTestMode.PROXY_ONLY,
                    bypass = null,
                    onProgress = null,
                    options = fastOptions
                )
                merged = mergePingResults(merged, proxyTested)
            }
        }

        val hash = storage.getContentHash().orEmpty()
        val traffic = storage.loadBuiltinTraffic()
        storage.saveConfigs(merged, hash, buildStableSourceStats(merged), traffic)

        val stillPending = merged.any { it.pingMs == null } ||
            (userMode != ConfigTestMode.TCP_ONLY && merged.any { it.isAvailable && !it.proxyVerified && it.connectSupported })
        !stillPending
    }

    /** Сохранить обновлённый список без перезагрузки с remote (для фонового пинга). */
    suspend fun saveConfigsChunk(configs: List<VpnConfig>) {
        val hash = storage.getContentHash().orEmpty()
        val traffic = storage.loadBuiltinTraffic()
        storage.saveConfigs(configs, hash, buildStableSourceStats(configs), traffic)
    }

    suspend fun clearAllData() {
        storage.clearAll()
        customStorage.saveAll(emptyList())
        lastUsedMirror = null
    }

    fun deduplicate(configs: List<VpnConfig>): List<VpnConfig> {
        val seen = linkedSetOf<String>()
        return configs.filter { seen.add(it.dedupeKey) }
    }

    private suspend fun saveFullCache(
        parsed: List<VpnConfig>,
        contentHash: String,
        traffic: Map<SubscriptionListType, SubscriptionTraffic>,
        onProgress: ((String) -> Unit)? = null
    ): RefreshResult.Parsed {
        val deduped = deduplicate(parsed).take(AppConstants.MAX_CACHE_CONFIGS)
        onProgress?.invoke("В кэше: ${deduped.size} конфигов")
        storage.saveConfigs(deduped, contentHash, buildStableSourceStats(deduped), traffic)
        appSettings.setAcknowledgedSubscriptionHash(contentHash)
        return RefreshResult.Parsed(deduped, contentHash, traffic)
    }

    private fun configsForScope(cache: List<VpnConfig>, scope: ConfigPingScope?): List<VpnConfig> =
        when (scope) {
            is ConfigPingScope.Custom -> cache.filter { it.customSourceId == scope.id }
            is ConfigPingScope.Builtin -> cache.filter { it.source == scope.type }
            null -> cache
        }

    private suspend fun refreshPingsInternal(
        onProgress: ((String) -> Unit)? = null,
        bypassProfile: ConnectionBypassProfile? = null,
        indexedProgress: ((Int, Int, String) -> Unit)? = null,
        fastSample: Boolean = false,
        forceMode: ConfigTestMode? = null,
        pingScope: ConfigPingScope? = null
    ): RefreshResult {
        val cache = loadCachedConfigs()
        val contentHash = storage.getContentHash().orEmpty()
        val traffic = storage.loadBuiltinTraffic()
        val scoped = configsForScope(cache, pingScope)
        val candidates = when (pingScope) {
            is ConfigPingScope.Custom, is ConfigPingScope.Builtin ->
                if (scoped.size <= AppConstants.MAX_CONFIGS_TO_TEST) scoped
                else sampleForTesting(scoped, fast = fastSample)
            null -> sampleForTesting(cache, fast = fastSample)
        }
        val scopeLabel = when (pingScope) {
            is ConfigPingScope.Custom -> "подписки"
            is ConfigPingScope.Builtin -> "списка"
            null -> "кэша"
        }
        onProgress?.invoke("Пинг $scopeLabel: ${candidates.size} из ${scoped.size}…")
        val testMode = forceMode ?: when (pingScope) {
            is ConfigPingScope.Custom, is ConfigPingScope.Builtin -> ConfigTestMode.TCP_ONLY
            null -> appSettings.getConfigTestMode()
        }
        val runOptions = ConfigTester.TestRunOptions(fast = fastSample)
        val tested = if (bypassProfile != null && (bypassProfile.dnsEnabled || bypassProfile.sniEnabled)) {
            tester.testConfigsWithBypass(
                configs = candidates,
                profile = bypassProfile,
                mode = testMode,
                onProgress = { _, _, label ->
                    indexedProgress?.invoke(0, 0, label)
                    onProgress?.invoke(label)
                },
                options = runOptions
            )
        } else {
            tester.testConfigs(candidates, testMode, bypassProfile, onProgress, runOptions)
        }
        val updated = mergePingResults(cache, tested)
        val stats = buildStableSourceStats(updated)
        storage.saveConfigs(updated, contentHash, stats, traffic)
        return RefreshResult.Success(
            configs = updated,
            groups = groupByCountry(ConfigFilters.filterForDisplay(updated, false)),
            mirrorUsed = lastUsedMirror,
            totalInSubscription = updated.size,
            loadedSources = SubscriptionListType.remoteSources.size + loadCustomSubscriptions().size,
            sourceStats = stats,
            builtinTraffic = traffic,
            pingPending = false
        )
    }

    private fun mergePingResults(
        cache: List<VpnConfig>,
        tested: List<VpnConfig>
    ): List<VpnConfig> {
        val testedByKey = tested.associateBy { it.dedupeKey }
        return cache.map { cfg -> testedByKey[cfg.dedupeKey] ?: cfg }
            .sortedBy { it.pingMs ?: Long.MAX_VALUE }
    }

    private fun buildStableSourceStats(configs: List<VpnConfig>): Map<SubscriptionListType, Int> {
        return SubscriptionListType.vpnSources.associateWith { type ->
            configs.count { it.source == type && ConfigFilters.isInSubscriptionList(it) }
        }
    }

    private fun sampleForTesting(configs: List<VpnConfig>, fast: Boolean = false): List<VpnConfig> {
        val maxTotal = if (fast) AppConstants.MAX_CONFIGS_TO_TEST_FAST else AppConstants.MAX_CONFIGS_TO_TEST
        val perSourceCap = if (fast) AppConstants.MAX_PER_SOURCE_SAMPLE_FAST else AppConstants.MAX_PER_SOURCE_SAMPLE
        val grouped = configs.groupBy { it.source ?: it.customSourceId }
        val perSource = minOf(
            perSourceCap,
            maxTotal / maxOf(1, grouped.size)
        )
        return grouped.values
            .flatMap { list ->
                val limit = sampleLimitForSource(list, perSource)
                sampleEvenly(list, limit)
            }
            .take(maxTotal)
    }

    /** Равномерная выборка по списку (не только первые N строк подписки). */
    private fun sampleEvenly(list: List<VpnConfig>, limit: Int): List<VpnConfig> {
        if (list.isEmpty() || limit <= 0) return emptyList()
        if (list.size <= limit) return list
        val step = list.size.toDouble() / limit
        return (0 until limit).map { i ->
            list[(i * step).toInt().coerceIn(0, list.lastIndex)]
        }
    }

    private fun sampleLimitForSource(list: List<VpnConfig>, defaultPerSource: Int): Int {
        if (list.isEmpty()) return 0
        return when {
            list.all { it.protocol == com.my.vpn.data.model.ConfigProtocol.SHADOWSOCKS } ->
                list.size.coerceAtMost(defaultPerSource.coerceAtLeast(80))
            list.any { it.protocol == com.my.vpn.data.model.ConfigProtocol.VMESS } ||
                list.any { it.protocol == com.my.vpn.data.model.ConfigProtocol.TROJAN } ->
                list.size.coerceAtMost(defaultPerSource.coerceAtLeast(150))
            else -> defaultPerSource
        }
    }

    private suspend fun buildDeferredSuccess(parsed: RefreshResult.Parsed): RefreshResult.Success {
        val stats = buildStableSourceStats(parsed.configs)
        return RefreshResult.Success(
            configs = parsed.configs,
            groups = groupByCountry(ConfigFilters.filterForDisplay(parsed.configs, allowPendingPing = true)),
            mirrorUsed = lastUsedMirror,
            totalInSubscription = parsed.configs.size,
            loadedSources = SubscriptionListType.remoteSources.size + loadCustomSubscriptions().size,
            sourceStats = stats,
            builtinTraffic = parsed.builtinTraffic,
            pingPending = true
        )
    }

    private suspend fun fetchAndParseAll(onProgress: ((String) -> Unit)?): RefreshResult {
        onProgress?.invoke("Загрузка всех подписок…")
        val fetched = fetchAllSubscriptions(onProgress)
        if (fetched.pairs.isEmpty() && fetched.customPairs.isEmpty()) {
            return RefreshResult.Error("Не удалось загрузить подписки. Проверьте интернет.")
        }
        val totalBuiltin = SubscriptionListType.remoteSources.size
        if (fetched.pairs.size in 1 until totalBuiltin) {
            onProgress?.invoke("Загружено ${fetched.pairs.size}/$totalBuiltin встроенных подписок (частично)")
        }

        onProgress?.invoke("Обработка подписок…")

        val builtinTraffic = mutableMapOf<SubscriptionListType, SubscriptionTraffic>()
        val allParsed = fetched.pairs.flatMap { (type, content) ->
            content.traffic?.let { builtinTraffic[type] = it }
            SubscriptionParser.parseSubscription(content.text)
                .map { it.copy(source = type) }
        } + fetched.customPairs.flatMap { (sub, content) ->
            content.traffic?.let { customStorage.updateTraffic(sub.id, it) }
            SubscriptionParser.parseSubscription(content.text)
                .map { it.copy(customSourceId = sub.id) }
        }

        if (allParsed.isEmpty()) {
            return RefreshResult.Error("Подписки пусты или формат не поддерживается")
        }

        val deduped = deduplicate(allParsed)
        val combinedHash = fetched.combinedHash ?: computeHashFromFetched(fetched.pairs)

        return RefreshResult.Parsed(deduped, combinedHash, builtinTraffic)
    }

    private suspend fun fetchAllSubscriptions(
        onProgress: ((String) -> Unit)? = null
    ): FetchBundle = coroutineScope {
        val customSubs = loadCustomSubscriptions()
        val totalBuiltin = SubscriptionListType.remoteSources.size
        val builtinPairs = SubscriptionListType.remoteSources.mapIndexed { index, type ->
            async {
                onProgress?.invoke("Загрузка ${type.title} (${index + 1}/$totalBuiltin)…")
                fetchRemoteContentForType(type)?.let { type to it }
            }
        }.awaitAll().filterNotNull()

        val customPairs = customSubs.mapIndexed { index, sub ->
            async {
                onProgress?.invoke("Своя подписка: ${sub.title} (${index + 1}/${customSubs.size})…")
                downloadUrl(sub.url)?.let { sub to it }
            }
        }.awaitAll().filterNotNull()

        val hash = if (builtinPairs.isNotEmpty()) {
            computeHashFromFetched(builtinPairs) +
                "|partial:${builtinPairs.size}/$totalBuiltin"
        } else {
            null
        }
        FetchBundle(builtinPairs, customPairs, hash)
    }

    private suspend fun fetchRemoteContentForType(
        subscription: SubscriptionListType
    ): RemoteContent? = withContext(Dispatchers.IO) {
        val preferred = storage.getPreferredMirror()
        val mirrors = buildMirrorOrder(preferred)

        // Не запускаем запросы ко всем зеркалам сразу: это даёт большой fan-out (N списков × M зеркал)
        // и часто приводит к троттлингу/задержкам. Вместо этого пробуем небольшими пакетами.
        coroutineScope {
            val batchSize = 4
            for (batch in mirrors.chunked(batchSize)) {
                val jobs: List<Deferred<Pair<MirrorType, RemoteContent>?>> = batch.map { mirror ->
                    async {
                        val url = SubscriptionUrls.buildUrl(mirror, subscription.fileName)
                        download(url, mirror)?.let { mirror to it }
                    }
                }
                while (jobs.any { it.isActive }) {
                    val hit = select<Pair<MirrorType, RemoteContent>?> {
                        jobs.filter { it.isActive }.forEach { job ->
                            job.onAwait { it }
                        }
                    } ?: continue
                    jobs.forEach { if (it.isActive) it.cancel() }
                    lastUsedMirror = hit.first
                    storage.savePreferredMirror(hit.first)
                    return@coroutineScope hit.second
                }
            }
            null
        }
    }

    private fun computeHashFromFetched(fetched: List<Pair<SubscriptionListType, RemoteContent>>): String {
        val combined = fetched
            .sortedBy { it.first.name }
            .joinToString("|") { "${it.first.name}:${it.second.hash}" }
        return sha256(combined)
    }

    private fun buildMirrorOrder(preferred: MirrorType?): List<MirrorType> {
        if (preferred == null) return SubscriptionUrls.defaultMirrorOrder
        return listOf(preferred) + SubscriptionUrls.defaultMirrorOrder.filter { it != preferred }
    }

    private fun download(url: String, mirror: MirrorType? = null): RemoteContent? {
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SputnikVPN/${AppConstants.APP_VERSION_NAME}")
                .header("Cache-Control", "no-cache")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val rawBody = response.body?.string().orEmpty()
                val body = com.my.vpn.util.MirrorContentNormalizer.normalize(rawBody)
                if (!isValidSubscriptionBody(body)) return@runCatching null
                val traffic = SubscriptionTrafficParser.fromUserInfoHeader(
                    response.header("subscription-userinfo")
                )
                RemoteContent(body, sha256(body), mirror, traffic)
            }
        }.getOrNull()
    }

    private fun downloadUrl(url: String): RemoteContent? = download(url, null)

    private fun isValidSubscriptionBody(body: String): Boolean {
        if (body.contains("://")) return true
        val trimmed = body.trim()
        if (trimmed.startsWith("[") && trimmed.contains("\"outbounds\"")) return true
        val lines = body.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        return runCatching {
            val decoded = String(Base64.getDecoder().decode(body.replace("\n", "").trim()), Charsets.UTF_8)
            decoded.contains("://") || (decoded.trimStart().startsWith("[") && decoded.contains("\"outbounds\""))
        }.getOrDefault(false)
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private data class FetchBundle(
        val pairs: List<Pair<SubscriptionListType, RemoteContent>>,
        val customPairs: List<Pair<CustomSubscription, RemoteContent>>,
        val combinedHash: String?
    )

    data class RemoteContent(
        val text: String,
        val hash: String,
        val mirror: MirrorType?,
        val traffic: SubscriptionTraffic? = null
    )

    sealed class RefreshResult {
        data class Parsed(
            val configs: List<VpnConfig>,
            val contentHash: String,
            val builtinTraffic: Map<SubscriptionListType, SubscriptionTraffic>
        ) : RefreshResult()

        data class Success(
            val configs: List<VpnConfig>,
            val groups: List<CountryGroup>,
            val mirrorUsed: MirrorType? = null,
            val totalInSubscription: Int = 0,
            val loadedSources: Int = 0,
            val sourceStats: Map<SubscriptionListType, Int> = emptyMap(),
            val builtinTraffic: Map<SubscriptionListType, SubscriptionTraffic> = emptyMap(),
            val pingPending: Boolean = false
        ) : RefreshResult()

        data class Error(val message: String) : RefreshResult()
    }
}
