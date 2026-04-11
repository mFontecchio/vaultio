package com.mrhayami.vaultio.data.repository

import android.util.Log
import com.mrhayami.vaultio.data.PokemonUtils
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.VintageSets
import com.mrhayami.vaultio.data.local.*
import com.mrhayami.vaultio.data.remote.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.async
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.delay

private const val TAG = "VaultioRepository"

class VaultioRepository(
    private val setDao: SetDao,
    private val cardDao: CardDao,
    private val userCardDao: UserCardDao,
    private val folderDao: FolderDao,
    private val priceDao: PriceDao,
    private val apiUsageDao: ApiUsageDao,
    private val telemetryDao: TelemetryDao,
    private val tcgDexApi: TcgDexApi,
    val justTcgApi: JustTcgApi,
    val userPreferencesRepository: UserPreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val moshi = Moshi.Builder().build()
    private val listIntAdapter = moshi.adapter<List<Int>>(Types.newParameterizedType(List::class.java, Int::class.javaObjectType))

    val allSets: Flow<List<SetEntity>> = setDao.getAllSets()
    val allUserCards: Flow<List<CardWithDetails>> = userCardDao.getAllUserCardsWithDetails()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val allFolderCardCrossRefs: Flow<List<FolderCardCrossRef>> = userCardDao.getAllFolderCardCrossRefs()
    val allPrices: Flow<List<PriceEntity>> = priceDao.getAllPrices()
    val allVintagePrices: Flow<List<VintagePriceEntity>> = priceDao.getAllVintagePrices()

    fun getUserCardById(userCardId: Long): Flow<CardWithDetails?> = userCardDao.getUserCardById(userCardId)

    fun getPricesForCard(cardId: String): Flow<List<PriceEntity>> = priceDao.getPricesForCard(cardId)

    fun getVintagePricesForCard(cardId: String): Flow<List<VintagePriceEntity>> = priceDao.getVintagePricesForCard(cardId)

    suspend fun refreshSets() {
        runCatching {
            val currentSets = setDao.getSetsSync()
            val downloadedSetIds = currentSets.filter { it.isDownloaded }.map { it.id }.toSet()
            
            val remoteSets = tcgDexApi.getSets()
            val entities = remoteSets.map { it.toEntity(downloadedSetIds.contains(it.id)) }
            setDao.insertSets(entities)
        }.onFailure { e ->
            Log.e(TAG, "Error refreshing sets", e)
        }
    }

    private fun TcgDexSet.toEntity(isDownloaded: Boolean) = SetEntity(
        id = id,
        name = name,
        series = series,
        logo = ensureImageUrl(logo ?: "https://assets.tcgdex.net/en/sets/$id/logo"),
        symbol = ensureImageUrl(symbol ?: "https://assets.tcgdex.net/en/sets/$id/symbol"),
        totalCards = cardCount.total,
        officialCards = cardCount.official,
        releaseDate = releaseDate,
        isDownloaded = isDownloaded
    )

    private fun ensureImageUrl(url: String): String {
        if (url.isEmpty()) return url
        val extensions = listOf(".png", ".webp", ".jpg", ".jpeg")
        return if (extensions.any { url.lowercase().endsWith(it) }) url else "$url.png"
    }

    suspend fun downloadSet(setId: String) {
        runCatching {
            val detail = tcgDexApi.getSetDetail(setId)
            val cardEntities = detail.cards.map { it.toEntity(setId) }
            cardDao.insertCards(cardEntities)
            setDao.updateDownloadStatus(setId, true)
            
            // Ensure set entity has icons
            val currentSet = setDao.getSetById(setId)
            if (currentSet?.logo?.contains("http") == false) {
                refreshSets() 
            }
        }.onFailure { e ->
            Log.e(TAG, "Error download set $setId", e)
        }
    }

    private fun TcgDexCard.toEntity(setId: String): CardEntity {
        val apiDexIds = dexId?.takeIf { it.isNotEmpty() }
        val resolvedDexIds = apiDexIds ?: if (category?.isPokemonCategory() == true) {
            PokemonUtils.lookupDexIds(name).takeIf { it.isNotEmpty() }
        } else null
        
        return CardEntity(
            id = id,
            localId = localId,
            name = name,
            image = image,
            setId = setId,
            rarity = rarity,
            category = category,
            types = types?.joinToString(","),
            dexId = resolvedDexIds?.firstOrNull()?.toString(),
            dexIds = resolvedDexIds?.let { listIntAdapter.toJson(it) },
            pokemonName = PokemonUtils.extractPokemonName(name),
            tcgPlayerId = extractTcgPlayerId(pricing?.tcgplayer?.url)
        )
    }

    private fun String.isPokemonCategory() = this == "Pokemon" || this == "Pokémon"

    suspend fun deleteDownloadedSet(setId: String) {
        setDao.updateDownloadStatus(setId, false)
        cardDao.deleteCardsBySet(setId)
    }

    suspend fun searchLocalCards(localId: String): List<CardEntity> = cardDao.getCardsByLocalId(localId)

    suspend fun searchLocalCardsWithTotal(localId: String, total: Int): List<CardEntity> = 
        cardDao.getCardsByLocalIdAndSetTotal(localId, total)

    suspend fun searchTcgDex(query: String): List<TcgDexCard> {
        if (query.isBlank()) return emptyList()
        return runCatching {
            tcgDexApi.searchCards("$query*")
        }.getOrElse { e ->
            Log.e(TAG, "Error searching cards", e)
            emptyList()
        }
    }

    suspend fun searchTcgDexByLocalId(localId: String): List<TcgDexCard> = runCatching {
        tcgDexApi.searchCardsByLocalId(localId)
    }.getOrElse { e ->
        Log.e(TAG, "Error searching by local ID", e)
        emptyList()
    }

    suspend fun getCardDetail(cardId: String): TcgDexCard? = runCatching {
        tcgDexApi.getCardDetail(cardId)
    }.getOrElse { e ->
        Log.e(TAG, "Error fetching card detail for $cardId", e)
        null
    }

    suspend fun addUserCard(card: TcgDexCard, userCardEntity: UserCardEntity, folderIds: List<Long> = emptyList()) {
        Log.d("Vaultio", "Adding card to collection: ${card.name} (${card.id})")
        val setId = card.id.substringBefore("-")
        ensureSetIsSynced(setId)

        val existingCard = cardDao.getCardById(card.id)
        
        if (existingCard?.dexId == null) {
            val fullCard = fetchFullCardDetails(card)
            val (dexIdString, dexIdsJson) = resolveDexIds(fullCard)

            val cardEntity = CardEntity(
                id = fullCard.id,
                localId = fullCard.localId,
                name = fullCard.name,
                image = fullCard.image,
                setId = setId,
                rarity = fullCard.rarity,
                category = fullCard.category,
                types = fullCard.types?.joinToString(","),
                dexId = dexIdString,
                dexIds = dexIdsJson,
                pokemonName = PokemonUtils.extractPokemonName(fullCard.name),
                tcgPlayerId = extractTcgPlayerId(fullCard.pricing?.tcgplayer?.url)
            )
            cardDao.insertCards(listOf(cardEntity))
        }

        val userCardIdResult = userCardDao.insertUserCard(userCardEntity.copy(cardId = card.id))

        if (folderIds.isNotEmpty()) {
            val crossRefs = folderIds.map { FolderCardCrossRef(folderId = it, userCardId = userCardIdResult) }
            userCardDao.insertFolderCardCrossRefs(crossRefs)
        }

        updateCardPrice(card.id)
    }

    private suspend fun ensureSetIsSynced(setId: String) {
        val setEntity = setDao.getSetById(setId)
        if (setEntity?.logo?.contains("http") != true) {
            runCatching {
                val remoteSets = tcgDexApi.getSets()
                remoteSets.find { it.id == setId }?.let { remoteSet ->
                    setDao.insertSets(listOf(remoteSet.toEntity(setEntity?.isDownloaded ?: false)))
                }
            }.onFailure { Log.e(TAG, "Failed to sync set", it) }
        }
    }

    private suspend fun fetchFullCardDetails(card: TcgDexCard): TcgDexCard {
        return if (card.dexId.isNullOrEmpty()) {
            getCardDetail(card.id) ?: card
        } else card
    }

    private suspend fun resolveDexIds(fullCard: TcgDexCard): Pair<String?, String?> {
        var dexIdString = fullCard.dexId?.firstOrNull()?.toString()
        var dexIdsJson = fullCard.dexId?.let { listIntAdapter.toJson(it) }

        if (dexIdString == null && fullCard.category?.isPokemonCategory() == true) {
            val normalizedName = PokemonUtils.extractPokemonName(fullCard.name)
            
            // Try local recovery
            val localMatch = cardDao.getCardsByPokemonName(normalizedName).firstOrNull()
                ?: cardDao.searchCardsByName(normalizedName).find { it.dexId != null }

            if (localMatch != null) {
                dexIdString = localMatch.dexId
                dexIdsJson = localMatch.dexIds
            } else {
                // Try network recovery
                val networkDex = attemptNetworkDexRecovery(normalizedName)
                if (networkDex != null) {
                    dexIdString = networkDex.first
                    dexIdsJson = networkDex.second
                } else {
                    // Static fallback
                    val staticIds = PokemonUtils.lookupDexIds(fullCard.name)
                    if (staticIds.isNotEmpty()) {
                        dexIdString = staticIds.first().toString()
                        dexIdsJson = listIntAdapter.toJson(staticIds)
                    }
                }
            }
        }
        return dexIdString to dexIdsJson
    }

    private suspend fun attemptNetworkDexRecovery(name: String): Pair<String, String>? {
        return runCatching {
            val searchResults = tcgDexApi.searchCards(name)
            for (shortCard in searchResults.take(10)) {
                if (shortCard.name.contains(name, ignoreCase = true)) {
                    val detail = if (shortCard.dexId.isNullOrEmpty()) getCardDetail(shortCard.id) else shortCard
                    detail?.dexId?.takeIf { it.isNotEmpty() }?.let { ids ->
                        return ids.first().toString() to listIntAdapter.toJson(ids)
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun extractTcgPlayerId(url: String?): String? {
        if (url == null) return null
        return Regex("""product/(\d+)""").find(url)?.groupValues?.get(1)
    }

    suspend fun updateUserCard(userCard: UserCardEntity) {
        userCardDao.insertUserCard(userCard)
    }

    suspend fun deleteUserCard(userCardId: Long) {
        userCardDao.deleteUserCard(userCardId)
    }

    suspend fun deleteUserCards(userCardIds: List<Long>) {
        userCardDao.deleteUserCards(userCardIds)
    }

    suspend fun addFolder(name: String, icon: String?, color: String?) {
        folderDao.insertFolder(FolderEntity(name = name, icon = icon, color = color))
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    suspend fun addCardToFolder(userCardId: Long, folderId: Long) {
        userCardDao.insertFolderCardCrossRef(FolderCardCrossRef(folderId = folderId, userCardId = userCardId))
    }

    suspend fun addCardsToFolder(userCardIds: List<Long>, folderId: Long) {
        val crossRefs = userCardIds.map { FolderCardCrossRef(folderId = folderId, userCardId = it) }
        userCardDao.insertFolderCardCrossRefs(crossRefs)
    }

    suspend fun removeCardFromFolder(userCardId: Long, folderId: Long) {
        userCardDao.removeCardFromFolder(userCardId, folderId)
    }

    suspend fun updateCardPrice(cardId: String) {
        val card = cardDao.getCardById(cardId) ?: return
        
        if (VintageSets.isVintageSet(card.setId)) {
            updateVintageCardPrice(card)
            return
        }

        val tcgDexSuccess = runCatching {
            val startTime = System.currentTimeMillis()
            val tcgDexCard = tcgDexApi.getCardDetail(cardId)
            logTelemetry("tcgdex", "cards/$cardId", 200, System.currentTimeMillis() - startTime)
            
            tcgDexCard.pricing?.tcgplayer?.let { pricing ->
                val entities = PricingUtils.mapTcgDexPrices(cardId, pricing)
                if (entities.isNotEmpty()) {
                    priceDao.insertPrices(entities)
                    true
                } else false
            } ?: false
        }.getOrElse { e ->
            Log.e(TAG, "TCGdex fetch failed for $cardId", e)
            logTelemetry("tcgdex", "cards/$cardId", 500, 0)
            false
        }

        if (!tcgDexSuccess) {
            updateCardPriceFromJustTCG(card)
        }
    }

    private suspend fun getJustTcgApiKey(): String? = userPreferencesRepository.justTcgApiKey.firstOrNull()

    private suspend fun updateVintageCardPrice(card: CardEntity) {
        val apiKey = getJustTcgApiKey() ?: return
        if (!canUseJustTcg()) return

        runCatching {
            val config = VintageSets.getVintageConfig(card.setId) ?: return
            val slugs = mutableSetOf<String>().apply {
                add(config.justTcgSetId)
                config.shadowlessJustTcgSetId?.let { add(it) }
            }

            val allVariants = mutableListOf<Pair<JustTcgVariant, String>>()
            val normalizedNumber = PricingUtils.normalizeCardNumber(card.localId)

            for (slug in slugs) {
                val startTime = System.currentTimeMillis()
                val response = justTcgApi.searchCards(
                    apiKey = apiKey,
                    query = card.name,
                    number = normalizedNumber,
                    set = slug
                )
                logTelemetry("justtcg", "cards/search", 200, System.currentTimeMillis() - startTime)
                syncApiUsage(response.metadata)
                
                response.data.forEach { jCard ->
                    jCard.variants.forEach { variant -> allVariants.add(variant to slug) }
                }
                if (slugs.size > 1) delay(500) 
            }

            if (allVariants.isNotEmpty()) {
                val vintagePrices = allVariants.mapNotNull { (variant, slug) ->
                    val targetPrintingValue = if (slug == config.shadowlessJustTcgSetId) {
                        if (variant.printing.lowercase().contains("1st edition")) 
                            PricingUtils.PRINTING_1ST_EDITION else PricingUtils.PRINTING_SHADOWLESS
                    } else null
                    PricingUtils.mapJustTcgVariantToVintagePrice(card.id, variant, targetPrintingValue)
                }
                if (vintagePrices.isNotEmpty()) priceDao.insertVintagePrices(vintagePrices)
            }
        }.onFailure { e ->
            Log.e(TAG, "JustTCG vintage fetch failed for ${card.id}", e)
            logTelemetry("justtcg", "cards", 500, 0)
        }
    }

    private suspend fun updateCardPriceFromJustTCG(card: CardEntity) {
        val apiKey = getJustTcgApiKey() ?: return
        if (!canUseJustTcg()) return

        runCatching {
            val startTime = System.currentTimeMillis()
            val normalizedNumber = PricingUtils.normalizeCardNumber(card.localId)
            
            val response = if (card.tcgPlayerId != null) {
                justTcgApi.getCardByTcgPlayerId(apiKey, card.tcgPlayerId)
            } else {
                justTcgApi.searchCards(
                    apiKey = apiKey,
                    query = card.name,
                    number = normalizedNumber
                )
            }
            
            logTelemetry("justtcg", "cards", 200, System.currentTimeMillis() - startTime)
            syncApiUsage(response.metadata)

            response.data.firstOrNull()?.let { justTcgCard ->
                val prices = justTcgCard.variants.mapNotNull { PricingUtils.mapJustTcgVariantToPrice(card.id, it) }
                if (prices.isNotEmpty()) priceDao.insertPrices(prices)
            }
        }.onFailure { e ->
            Log.e(TAG, "JustTCG fallback failed for ${card.id}", e)
            logTelemetry("justtcg", "cards", 500, 0)
        }
    }

    private suspend fun canUseJustTcg(): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val usage = apiUsageDao.getUsageForDate(today)
        return (usage?.dailyRemaining ?: 100) > 0
    }

    suspend fun updatePricesBatch(cards: List<CardEntity>) = supervisorScope {
        val (vintage, modern) = cards.partition { VintageSets.isVintageSet(it.setId) }
        
        val modernDeferred = modern.map { card ->
            async(ioDispatcher) { updateCardPrice(card.id) }
        }
        
        val vintageDeferred = vintage.map { card ->
            async(ioDispatcher) { updateVintageCardPrice(card) }
        }
        
        (modernDeferred + vintageDeferred).forEach { it.await() }
    }

    fun getApiUsageFlow(): Flow<ApiUsageEntity?> {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return apiUsageDao.getUsageFlow(today)
    }

    suspend fun getApiUsageDetails(): ApiUsageEntity? {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return apiUsageDao.getUsageForDate(today)
    }

    suspend fun syncApiUsage(metadata: JustTcgMetadata?) {
        if (metadata == null) return
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val entity = ApiUsageEntity(
            date = today,
            count = metadata.apiDailyRequestsUsed,
            dailyLimit = metadata.apiDailyLimit,
            dailyRemaining = metadata.apiDailyRequestsRemaining,
            planLimit = metadata.apiRequestLimit,
            planUsed = metadata.apiRequestsUsed,
            planRemaining = metadata.apiRequestsRemaining,
            planName = metadata.apiPlan,
            lastSyncedAt = System.currentTimeMillis()
        )
        apiUsageDao.insertUsage(entity)
    }

    suspend fun refreshApiUsageFromApi(): Boolean {
        val apiKey = getJustTcgApiKey() ?: return false
        return runCatching {
            val response = justTcgApi.searchCards(
                apiKey = apiKey, 
                query = "pikachu", 
                limit = 1
            )
            syncApiUsage(response.metadata)
            true
        }.getOrElse { false }
    }

    suspend fun logTelemetry(api: String, endpoint: String, status: Int, latency: Long) {
        telemetryDao.insertLog(TelemetryLogEntity(api = api, endpoint = endpoint, status = status, latency = latency))
    }
}
