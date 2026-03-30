package com.mrhayami.vaultio.data.repository

import android.util.Log
import com.mrhayami.vaultio.data.PokemonUtils
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.VintageSets
import com.mrhayami.vaultio.data.local.*
import com.mrhayami.vaultio.data.remote.JustTcgApi
import com.mrhayami.vaultio.data.remote.JustTcgBatchRequestItem
import com.mrhayami.vaultio.data.remote.JustTcgMetadata
import com.mrhayami.vaultio.data.remote.JustTcgVariant
import com.mrhayami.vaultio.data.remote.TcgDexApi
import com.mrhayami.vaultio.data.remote.TcgDexCard
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

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
    val userPreferencesRepository: UserPreferencesRepository
) {
    private val moshi = Moshi.Builder().build()
    private val listIntAdapter = moshi.adapter<List<Int>>(Types.newParameterizedType(List::class.java, Int::class.javaObjectType))

    val allSets: Flow<List<SetEntity>> = setDao.getAllSets()
    val allUserCards: Flow<List<CardWithDetails>> = userCardDao.getAllUserCardsWithDetails()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val allFolderCardCrossRefs: Flow<List<FolderCardCrossRef>> = userCardDao.getAllFolderCardCrossRefs()
    val allPrices: Flow<List<PriceEntity>> = priceDao.getAllPrices()
    val allVintagePrices: Flow<List<VintagePriceEntity>> = priceDao.getAllVintagePrices()

    fun getUserCardById(userCardId: Long): Flow<CardWithDetails?> {
        return userCardDao.getUserCardById(userCardId)
    }

    fun getUserCardsByFolder(folderId: Long): Flow<List<CardWithDetails>> {
        return userCardDao.getUserCardsByFolder(folderId)
    }

    fun getPricesForCard(cardId: String): Flow<List<PriceEntity>> {
        return priceDao.getPricesForCard(cardId)
    }

    fun getVintagePricesForCard(cardId: String): Flow<List<VintagePriceEntity>> {
        return priceDao.getVintagePricesForCard(cardId)
    }

    suspend fun refreshSets() {
        try {
            val currentSets = setDao.getSetsSync()
            val downloadedSetIds = currentSets.filter { it.isDownloaded }.map { it.id }.toSet()
            
            val remoteSets = tcgDexApi.getSets()
            val entities = remoteSets.map {
                val setId = it.id
                SetEntity(
                    id = setId,
                    name = it.name,
                    series = it.series,
                    logo = ensureImageUrl(it.logo ?: "https://assets.tcgdex.net/en/sets/$setId/logo"),
                    symbol = ensureImageUrl(it.symbol ?: "https://assets.tcgdex.net/en/sets/$setId/symbol"),
                    totalCards = it.cardCount.total,
                    officialCards = it.cardCount.official,
                    releaseDate = it.releaseDate,
                    isDownloaded = downloadedSetIds.contains(setId)
                )
            }
            setDao.insertSets(entities)
        } catch (e: Exception) {
            Log.e("VaultioRepository", "Error refreshing sets", e)
        }
    }

    private fun ensureImageUrl(url: String): String {
        if (url.isEmpty()) return url
        val extensions = listOf(".png", ".webp", ".jpg", ".jpeg")
        return if (extensions.any { url.lowercase().endsWith(it) }) {
            url
        } else {
            "$url.png"
        }
    }

    suspend fun downloadSet(setId: String) {
        try {
            val detail = tcgDexApi.getSetDetail(setId)
            val cardEntities = detail.cards.map {
                CardEntity(
                    id = it.id,
                    localId = it.localId,
                    name = it.name,
                    image = it.image,
                    setId = setId,
                    rarity = it.rarity,
                    category = it.category,
                    types = it.types?.joinToString(","),
                    dexId = it.dexId?.firstOrNull()?.toString(),
                    dexIds = it.dexId?.let { ids -> listIntAdapter.toJson(ids) },
                    pokemonName = PokemonUtils.extractPokemonName(it.name),
                    tcgPlayerId = extractTcgPlayerId(it.pricing?.tcgplayer?.url)
                )
            }
            cardDao.insertCards(cardEntities)
            setDao.updateDownloadStatus(setId, true)
            
            // Ensure set entity has icons
            val currentSet = setDao.getSetById(setId)
            if (currentSet != null && (currentSet.logo == null || !currentSet.logo.contains("http"))) {
                refreshSets() 
            }
        } catch (e: Exception) {
            Log.e("VaultioRepository", "Error download set $setId", e)
        }
    }

    suspend fun deleteDownloadedSet(setId: String) {
        setDao.updateDownloadStatus(setId, false)
        cardDao.deleteCardsBySet(setId)
    }

    suspend fun searchLocalCards(localId: String): List<CardEntity> {
        return cardDao.getCardsByLocalId(localId)
    }

    suspend fun searchLocalCardsWithTotal(localId: String, total: Int): List<CardEntity> {
        return cardDao.getCardsByLocalIdAndSetTotal(localId, total)
    }

    suspend fun searchTcgDex(query: String): List<TcgDexCard> {
        return try {
            if (query.isBlank()) return emptyList()
            tcgDexApi.searchCards("$query*")
        } catch (e: Exception) {
            Log.e("VaultioRepository", "Error searching cards", e)
            emptyList()
        }
    }

    suspend fun searchTcgDexByLocalId(localId: String): List<TcgDexCard> {
        return try {
            tcgDexApi.searchCardsByLocalId(localId)
        } catch (e: Exception) {
            Log.e("VaultioRepository", "Error searching by local ID", e)
            emptyList()
        }
    }

    suspend fun getCardDetail(cardId: String): TcgDexCard? {
        return try {
            tcgDexApi.getCardDetail(cardId)
        } catch (e: Exception) {
            Log.e("VaultioRepository", "Error fetching card detail for $cardId", e)
            null
        }
    }

    suspend fun addUserCard(card: TcgDexCard, userCardEntity: UserCardEntity, folderIds: List<Long> = emptyList()) {
        Log.d("Vaultio", "Adding card to collection: ${card.name} (${card.id})")
        val setId = card.id.substringBefore("-")
        var setEntity = setDao.getSetById(setId)
        if (setEntity == null || setEntity.logo == null || !setEntity.logo.contains("http")) {
            try {
                val remoteSets = tcgDexApi.getSets()
                val remoteSet = remoteSets.find { it.id == setId }
                if (remoteSet != null) {
                    val updatedEntity = SetEntity(
                        id = remoteSet.id,
                        name = remoteSet.name,
                        series = remoteSet.series,
                        logo = ensureImageUrl(remoteSet.logo ?: "https://assets.tcgdex.net/en/sets/$setId/logo"),
                        symbol = ensureImageUrl(remoteSet.symbol ?: "https://assets.tcgdex.net/en/sets/$setId/symbol"),
                        totalCards = remoteSet.cardCount.total,
                        officialCards = remoteSet.cardCount.official,
                        releaseDate = remoteSet.releaseDate,
                        isDownloaded = setEntity?.isDownloaded ?: false
                    )
                    setDao.insertSets(listOf(updatedEntity))
                    setEntity = updatedEntity
                }
            } catch (e: Exception) { Log.e("Vaultio", "Failed to sync set", e) }
        }

        val existingCard = cardDao.getCardById(card.id)
        
        // If the card doesn't exist or is missing its dexId, ensure we have full details and update the DB
        if (existingCard == null || existingCard.dexId == null) {
            val fullCard = if (card.dexId.isNullOrEmpty()) {
                try {
                    Log.d("Vaultio", "Fetching details for ${card.id} to ensure dexId presence")
                    tcgDexApi.getCardDetail(card.id)
                } catch (e: Exception) {
                    Log.e("Vaultio", "Failed to fetch card details", e)
                    card
                }
            } else {
                card
            }

            var dexIdString = fullCard.dexId?.firstOrNull()?.toString()
            var dexIdsJson = fullCard.dexId?.let { listIntAdapter.toJson(it) }

            // Recovery: If dexId is missing in API, check local and then network for a valid dexId for this name
            if (dexIdString == null && (fullCard.category == "Pokemon" || fullCard.category == "Pokémon")) {
                val normalizedName = PokemonUtils.extractPokemonName(fullCard.name)
                Log.d("Vaultio", "DexID missing for ${fullCard.name}. Attempting recovery with normalized name: $normalizedName")
                
                val candidates = cardDao.searchCardsByName(normalizedName)
                val localMatch = candidates.find { 
                    (it.name.contains(normalizedName, ignoreCase = true) || it.pokemonName?.equals(normalizedName, ignoreCase = true) == true) 
                    && it.dexId != null 
                }
                
                if (localMatch != null) {
                    dexIdString = localMatch.dexId
                    dexIdsJson = localMatch.dexIds
                    Log.d("Vaultio", "Recovered dexId $dexIdString for ${fullCard.name} from local card ${localMatch.id}")
                } else {
                    Log.d("Vaultio", "Local recovery failed for ${fullCard.name}. Trying network recovery with: $normalizedName")
                    try {
                        // Try to find any version of this card on the network that has a dexId
                        val searchResults = tcgDexApi.searchCards(normalizedName)
                        for (shortCard in searchResults.take(10)) { // Check top 10 results
                            if (shortCard.name.contains(normalizedName, ignoreCase = true)) {
                                if (!shortCard.dexId.isNullOrEmpty()) {
                                    dexIdString = shortCard.dexId.first().toString()
                                    dexIdsJson = listIntAdapter.toJson(shortCard.dexId)
                                    Log.d("Vaultio", "Network recovery SUCCESS: found dexId $dexIdString in search result for ${shortCard.id}")
                                    break
                                } else {
                                    // Try fetching details for this other version to see if IT has a dexId
                                    val otherDetail = try { tcgDexApi.getCardDetail(shortCard.id) } catch (e: Exception) { null }
                                    val otherDexId = otherDetail?.dexId?.firstOrNull()?.toString()
                                    if (otherDexId != null) {
                                        dexIdString = otherDexId
                                        dexIdsJson = otherDetail.dexId?.let { listIntAdapter.toJson(it) }
                                        Log.d("Vaultio", "Network recovery SUCCESS: found dexId $dexIdString via detail of ${shortCard.id}")
                                        break
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Vaultio", "Network recovery failed", e)
                    }
                }
            }

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
            Log.d("Vaultio", "Inserting CardEntity: id=${cardEntity.id}, name=${cardEntity.name}, dexId=${cardEntity.dexId}")
            cardDao.insertCards(listOf(cardEntity))
        } else {
            Log.d("Vaultio", "Card already exists in local DB with dexId: ${existingCard.dexId}")
        }
        val userCardIdResult = userCardDao.insertUserCard(userCardEntity.copy(cardId = card.id))
        Log.d("Vaultio", "Inserted UserCard with result ID: $userCardIdResult")

        if (folderIds.isNotEmpty()) {
            val crossRefs = folderIds.map { FolderCardCrossRef(folderId = it, userCardId = userCardIdResult) }
            userCardDao.insertFolderCardCrossRefs(crossRefs)
            Log.d("Vaultio", "Added UserCard $userCardIdResult to folders: $folderIds")
        }

        // Trigger individual price update for the new card
        updateCardPrice(card.id)
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

    // --- Pricing Logic Implementation ---

    suspend fun updateCardPrice(cardId: String) {
        val card = cardDao.getCardById(cardId) ?: return
        
        // 1. Vintage Logic (Base Set - Neo Destiny)
        if (VintageSets.isVintageSet(card.setId)) {
            Log.d("VaultioRepository", "Card ${card.id} is from a vintage set. Using JustTCG.")
            updateVintageCardPrice(card)
            return
        }

        // 2. Primary Source: TCGdex
        try {
            val startTime = System.currentTimeMillis()
            val tcgDexCard = tcgDexApi.getCardDetail(cardId)
            logTelemetry("tcgdex", "cards/$cardId", 200, System.currentTimeMillis() - startTime)
            
            val tcgPlayerPricing = tcgDexCard.pricing?.tcgplayer
            if (tcgPlayerPricing != null) {
                val entities = PricingUtils.mapTcgDexPrices(cardId, tcgPlayerPricing)
                if (entities.isNotEmpty()) {
                    priceDao.insertPrices(entities)
                    return // Success with TCGdex
                }
            }
        } catch (e: Exception) {
            Log.e("VaultioRepository", "TCGdex fetch failed for $cardId", e)
            logTelemetry("tcgdex", "cards/$cardId", 500, 0)
        }

        // 3. Fallback: JustTCG
        updateCardPriceFromJustTCG(card)
    }

    private suspend fun getJustTcgApiKey(): String? {
        return userPreferencesRepository.justTcgApiKey.firstOrNull()
    }

    private suspend fun updateVintageCardPrice(card: CardEntity) {
        val apiKey = getJustTcgApiKey() ?: return
        if (!canUseJustTcg()) return

        try {
            val config = VintageSets.getVintageConfig(card.setId) ?: return
            val slugs = mutableSetOf<String>()
            slugs.add(config.justTcgSetId)
            config.shadowlessJustTcgSetId?.let { slugs.add(it) }

            val allVariants = mutableListOf<Pair<JustTcgVariant, String>>() // Variant and the slug it came from
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
                    jCard.variants.forEach { variant ->
                        allVariants.add(variant to slug)
                    }
                }
                
                if (slugs.size > 1) delay(500) 
            }

            if (allVariants.isNotEmpty()) {
                val vintagePrices = allVariants.mapNotNull { (variant, slug) ->
                    // Disambiguate Shadowless for Base Set
                    val targetPrintingValue = if (slug == config.shadowlessJustTcgSetId) {
                        val is1stEd = variant.printing.lowercase().contains("1st edition")
                        if (is1stEd) PricingUtils.PRINTING_1ST_EDITION else PricingUtils.PRINTING_SHADOWLESS
                    } else {
                        null // use default parsing
                    }
                    
                    PricingUtils.mapJustTcgVariantToVintagePrice(card.id, variant, targetPrintingValue)
                }
                if (vintagePrices.isNotEmpty()) {
                    priceDao.insertVintagePrices(vintagePrices)
                }
            }
        } catch (e: Exception) {
            Log.e("VaultioRepository", "JustTCG vintage fetch failed for ${card.id}", e)
            logTelemetry("justtcg", "cards", 500, 0)
        }
    }

    private suspend fun updateCardPriceFromJustTCG(card: CardEntity) {
        val apiKey = getJustTcgApiKey() ?: return
        if (!canUseJustTcg()) return

        try {
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
            syncApiUsage(metadata = response.metadata)

            val justTcgCard = response.data.firstOrNull()
            if (justTcgCard != null) {
                val prices = justTcgCard.variants.mapNotNull { 
                    PricingUtils.mapJustTcgVariantToPrice(card.id, it)
                }
                if (prices.isNotEmpty()) {
                    priceDao.insertPrices(prices)
                }
            }
        } catch (e: Exception) {
            Log.e("VaultioRepository", "JustTCG fallback failed for ${card.id}", e)
            logTelemetry("justtcg", "cards", 500, 0)
        }
    }

    private suspend fun canUseJustTcg(): Boolean {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val usage = apiUsageDao.getUsageForDate(today)
        return (usage?.dailyRemaining ?: 100) > 0
    }

    suspend fun updatePricesBatch(cards: List<CardEntity>) {
        val apiKey = getJustTcgApiKey()
        val (vintage, modern) = cards.partition { VintageSets.isVintageSet(it.setId) }

        // Process Modern via TCGdex (Sequential for simplicity, could be parallel)
        modern.forEach { updateCardPrice(it.id) }

        // Process Vintage individually to handle 1st Ed / Shadowless nuances
        vintage.forEach { updateVintageCardPrice(it) }
    }

    suspend fun getApiUsage(): Int {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return apiUsageDao.getUsageForDate(today)?.count ?: 0
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
            planName = metadata.apiPlan
        )
        apiUsageDao.insertUsage(entity)
    }

    suspend fun incrementApiUsage() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currentUsage = apiUsageDao.getUsageForDate(today)
        if (currentUsage == null) {
            apiUsageDao.insertUsage(ApiUsageEntity(date = today, count = 1))
        } else {
            apiUsageDao.insertUsage(currentUsage.copy(
                count = currentUsage.count + 1,
                dailyRemaining = maxOf(0, currentUsage.dailyRemaining - 1),
                planUsed = currentUsage.planUsed + 1,
                planRemaining = maxOf(0, currentUsage.planRemaining - 1)
            ))
        }
    }

    suspend fun logTelemetry(api: String, endpoint: String, status: Int, latency: Long) {
        telemetryDao.insertLog(TelemetryLogEntity(api = api, endpoint = endpoint, status = status, latency = latency))
    }
}
