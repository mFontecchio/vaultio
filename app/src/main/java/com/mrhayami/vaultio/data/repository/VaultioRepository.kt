package com.mrhayami.vaultio.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.mrhayami.vaultio.data.PokemonUtils
import com.mrhayami.vaultio.data.PricingUtils
import com.mrhayami.vaultio.data.UserPreferencesRepository
import com.mrhayami.vaultio.data.VintageSets
import com.mrhayami.vaultio.data.local.ApiUsageDao
import com.mrhayami.vaultio.data.local.ApiUsageEntity
import com.mrhayami.vaultio.data.local.CardDao
import com.mrhayami.vaultio.data.local.CardEntity
import com.mrhayami.vaultio.data.local.CardWithDetails
import com.mrhayami.vaultio.data.local.CollectionExportDto
import com.mrhayami.vaultio.data.local.CollectionSnapshotDao
import com.mrhayami.vaultio.data.local.CollectionSnapshotEntity
import com.mrhayami.vaultio.data.local.FolderCardCrossRef
import com.mrhayami.vaultio.data.local.FolderDao
import com.mrhayami.vaultio.data.local.FolderDto
import com.mrhayami.vaultio.data.local.FolderEntity
import com.mrhayami.vaultio.data.local.PriceDao
import com.mrhayami.vaultio.data.local.PriceEntity
import com.mrhayami.vaultio.data.local.SetDao
import com.mrhayami.vaultio.data.local.SetEntity
import com.mrhayami.vaultio.data.local.TelemetryDao
import com.mrhayami.vaultio.data.local.TelemetryLogEntity
import com.mrhayami.vaultio.data.local.UserCardDao
import com.mrhayami.vaultio.data.local.UserCardDto
import com.mrhayami.vaultio.data.local.UserCardEntity
import com.mrhayami.vaultio.data.local.VintagePriceEntity
import com.mrhayami.vaultio.data.local.WishlistCardEntity
import com.mrhayami.vaultio.data.local.WishlistCardWithDetails
import com.mrhayami.vaultio.data.local.WishlistDao
import com.mrhayami.vaultio.data.remote.JustTcgApi
import com.mrhayami.vaultio.data.remote.JustTcgMetadata
import com.mrhayami.vaultio.data.remote.JustTcgVariant
import com.mrhayami.vaultio.data.remote.TcgDexApi
import com.mrhayami.vaultio.data.remote.TcgDexCard
import com.mrhayami.vaultio.data.remote.TcgDexSet
import com.mrhayami.vaultio.ui.collection.FilterSettings
import com.mrhayami.vaultio.ui.collection.SortDirection
import com.mrhayami.vaultio.ui.collection.SortMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "VaultioRepository"

class VaultioRepository(
    private val setDao: SetDao,
    private val cardDao: CardDao,
    private val userCardDao: UserCardDao,
    private val folderDao: FolderDao,
    private val priceDao: PriceDao,
    private val apiUsageDao: ApiUsageDao,
    private val telemetryDao: TelemetryDao,
    private val collectionSnapshotDao: CollectionSnapshotDao,
    private val wishlistDao: WishlistDao,
    private val tcgDexApi: TcgDexApi,
    val justTcgApi: JustTcgApi,
    val userPreferencesRepository: UserPreferencesRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val moshi = Moshi.Builder().build()
    private val listIntAdapter = moshi.adapter<List<Int>>(Types.newParameterizedType(List::class.java, Int::class.javaObjectType))
    private val collectionExportAdapter = moshi.adapter(CollectionExportDto::class.java)

    val allSets: Flow<List<SetEntity>> = setDao.getAllSets()
    val allUserCards: Flow<List<CardWithDetails>> = userCardDao.getAllUserCardsWithDetails()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val allFolderCardCrossRefs: Flow<List<FolderCardCrossRef>> = userCardDao.getAllFolderCardCrossRefs()
    val allPrices: Flow<List<PriceEntity>> = priceDao.getAllPrices()
    val allVintagePrices: Flow<List<VintagePriceEntity>> = priceDao.getAllVintagePrices()
    val allWishlistCards: Flow<List<WishlistCardWithDetails>> = wishlistDao.getAllWishlistCards()

    fun getUserCardById(userCardId: Long): Flow<CardWithDetails?> = userCardDao.getUserCardById(userCardId)

    suspend fun getUserCardByIdSync(userCardId: Long): CardWithDetails? = userCardDao.getUserCardByIdSync(userCardId)

    fun getPricesForCard(cardId: String): Flow<List<PriceEntity>> = priceDao.getPricesForCard(cardId)

    fun getVintagePricesForCard(cardId: String): Flow<List<VintagePriceEntity>> = priceDao.getVintagePricesForCard(cardId)

    fun getFilteredUserCards(
        searchQuery: String,
        folderId: Long?,
        sortMode: SortMode,
        sortDirection: SortDirection,
        filterSettings: FilterSettings
    ): Flow<List<CardWithDetails>> {
        val queryBuilder = StringBuilder()
        val args = mutableListOf<Any>()

        queryBuilder.append(
            """
            SELECT
                uc.*,
                c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.dexIds as card_dexIds, c.pokemonName as card_pokemonName, c.tcgPlayerId as card_tcgPlayerId, c.pHash as card_pHash, c.lastUpdated as card_lastUpdated,
                s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
            FROM user_cards uc
            INNER JOIN cards c ON uc.cardId = c.id
            INNER JOIN sets s ON c.setId = s.id
        """
        )

        if (folderId != null) {
            queryBuilder.append(" INNER JOIN folder_cards fc ON uc.id = fc.userCardId ")
        }

        queryBuilder.append(" WHERE 1=1 ")

        if (folderId != null) {
            queryBuilder.append(" AND fc.folderId = ? ")
            args.add(folderId)
        }

        if (searchQuery.isNotBlank()) {
            queryBuilder.append(" AND (c.name LIKE ? OR c.pokemonName LIKE ? OR s.name LIKE ?) ")
            val likeQuery = "%$searchQuery%"
            args.add(likeQuery)
            args.add(likeQuery)
            args.add(likeQuery)
        }

        if (filterSettings.rarities.isNotEmpty()) {
            queryBuilder.append(" AND c.rarity IN (${filterSettings.rarities.joinToString { "?" }}) ")
            args.addAll(filterSettings.rarities)
        }

        if (filterSettings.categories.isNotEmpty()) {
            queryBuilder.append(" AND c.category IN (${filterSettings.categories.joinToString { "?" }}) ")
            args.addAll(filterSettings.categories)
        }

        if (filterSettings.conditions.isNotEmpty()) {
            queryBuilder.append(" AND uc.condition IN (${filterSettings.conditions.joinToString { "?" }}) ")
            args.addAll(filterSettings.conditions)
        }

        if (filterSettings.finishes.isNotEmpty()) {
            queryBuilder.append(" AND uc.finish IN (${filterSettings.finishes.joinToString { "?" }}) ")
            args.addAll(filterSettings.finishes)
        }

        if (filterSettings.types.isNotEmpty()) {
            // Types are comma-separated in DB
            queryBuilder.append(" AND (")
            filterSettings.types.forEachIndexed { index, type ->
                if (index > 0) queryBuilder.append(" OR ")
                queryBuilder.append(" c.types LIKE ? ")
                args.add("%$type%")
            }
            queryBuilder.append(") ")
        }

        val orderBy = when (sortMode) {
            SortMode.NAME -> "c.name"
            SortMode.SET -> "s.releaseDate ${if (sortDirection == SortDirection.DESCENDING) "DESC" else "ASC"}, c.localId"
            SortMode.VALUE -> "COALESCE(uc.manualPrice, 0.0)" // Note: Market prices are in a different table, so this only sorts by manual price or 0
            SortMode.DATE_ADDED -> "uc.dateAdded"
            SortMode.RARITY -> "c.rarity"
            SortMode.QUANTITY -> "uc.quantity"
            SortMode.NUMBER -> "c.localId"
        }

        val direction = if (sortDirection == SortDirection.DESCENDING) "DESC" else "ASC"

        // Custom handling for VALUE sorting if it's not manual price requires a more complex join
        // For now, we'll keep the ViewModel-side sorting for VALUE since it depends on the priceMap
        if (sortMode != SortMode.VALUE) {
            queryBuilder.append(" ORDER BY $orderBy $direction")
        }

        val query = SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
        return userCardDao.getFilteredUserCards(query)
    }

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

    private val okHttpClient = OkHttpClient.Builder().build()

    suspend fun updateCardPHash(cardId: String, pHash: Long) = withContext(ioDispatcher) {
        cardDao.updateCardPHash(cardId, pHash)
    }

    suspend fun fetchBitmapFromUrl(url: String): Bitmap? = withContext(ioDispatcher) {
        try {
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body ?: return@withContext null
                BitmapFactory.decodeStream(body.byteStream())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bitmap from $url", e)
            null
        }
    }

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

    suspend fun addUserCard(
        card: TcgDexCard,
        userCardEntity: UserCardEntity,
        folderIds: List<Long> = emptyList()
    ): Long {
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

        val existingUserCard = userCardDao.findExistingUserCard(
            cardId = card.id,
            condition = userCardEntity.condition,
            printing = userCardEntity.printing,
            finish = userCardEntity.finish
        )

        val userCardIdResult: Long = if (existingUserCard != null) {
            userCardDao.updateUserCard(existingUserCard.copy(quantity = existingUserCard.quantity + userCardEntity.quantity))
            existingUserCard.id
        } else {
            userCardDao.insertUserCard(userCardEntity.copy(cardId = card.id))
        }

        if (folderIds.isNotEmpty()) {
            val crossRefs = folderIds.map { FolderCardCrossRef(folderId = it, userCardId = userCardIdResult) }
            userCardDao.insertFolderCardCrossRefs(crossRefs)
        }

        updateCardPrice(card.id)
        return userCardIdResult
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
        userCardDao.updateUserCard(userCard)
    }

    suspend fun splitUserCard(userCardId: Long, newCondition: String, newPrinting: String, newFinish: String): Long? {
        return withContext(ioDispatcher) {
            val originalDetails = userCardDao.getUserCardByIdSync(userCardId) ?: return@withContext null
            val original = originalDetails.userCard
            
            if (original.quantity <= 1) return@withContext null

            // 1. Decrease quantity of original
            userCardDao.updateUserCard(original.copy(quantity = original.quantity - 1))

            // 2. Check if a card with the new attributes already exists
            val existing = userCardDao.findExistingUserCard(
                cardId = original.cardId,
                condition = newCondition,
                printing = newPrinting,
                finish = newFinish
            )

            val newId = if (existing != null) {
                // Add to existing
                userCardDao.updateUserCard(existing.copy(quantity = existing.quantity + 1))
                existing.id
            } else {
                // Create new entry
                val newEntry = UserCardEntity(
                    cardId = original.cardId,
                    quantity = 1,
                    condition = newCondition,
                    printing = newPrinting,
                    finish = newFinish,
                    dateAdded = System.currentTimeMillis()
                )
                val insertedId = userCardDao.insertUserCard(newEntry)
                
                // Copy folder associations from the original card
                val originalFolders = userCardDao.getFolderIdsForUserCardSync(userCardId)
                if (originalFolders.isNotEmpty()) {
                    userCardDao.insertFolderCardCrossRefs(originalFolders.map { 
                        FolderCardCrossRef(folderId = it, userCardId = insertedId) 
                    })
                }
                insertedId
            }
            
            // 3. Ensure price is updated for the new finish if needed
            updateCardPrice(original.cardId)
            
            newId
        }
    }

    suspend fun deleteUserCard(userCardId: Long) {
        userCardDao.deleteFolderCardCrossRefsForUserCard(userCardId)
        userCardDao.deleteUserCard(userCardId)
    }

    suspend fun deleteUserCards(userCardIds: List<Long>) {
        userCardIds.forEach { userCardDao.deleteFolderCardCrossRefsForUserCard(it) }
        userCardDao.deleteUserCards(userCardIds)
    }

    suspend fun deleteLastUserCardInstance(cardId: String) {
        withContext(ioDispatcher) {
            val lastCard = userCardDao.getLastUserCardByCardId(cardId)
            if (lastCard != null) {
                if (lastCard.quantity > 1) {
                    userCardDao.updateUserCard(lastCard.copy(quantity = lastCard.quantity - 1))
                } else {
                    deleteUserCard(lastCard.id)
                }
            }
        }
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun updatePricesBatch(cards: List<CardEntity>) = withContext(ioDispatcher) {
        val (vintage, modern) = cards.partition { VintageSets.isVintageSet(it.setId) }

        // Use a Flow with flatMapMerge to limit concurrency and avoid overloading the network/API
        val concurrency = 4

        (modern.map { it.id } + vintage.map { it.id }).asFlow()
            .flatMapMerge(concurrency = concurrency) { cardId ->
                flow {
                    val card = cards.find { it.id == cardId }
                    if (card != null) {
                        if (VintageSets.isVintageSet(card.setId)) {
                            updateVintageCardPrice(card)
                        } else {
                            updateCardPrice(card.id)
                        }
                    }
                    emit(Unit)
                }
            }
            .collect()
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

    suspend fun exportCollectionJson(folderIds: List<Long>? = null): String = withContext(ioDispatcher) {
        val folders = if (folderIds == null) {
            folderDao.getAllFolders().firstOrNull() ?: emptyList()
        } else {
            folderDao.getAllFolders().firstOrNull()?.filter { folderIds.contains(it.id) } ?: emptyList()
        }

        val allUserCards = userCardDao.getAllUserCardsWithDetails().firstOrNull() ?: emptyList()
        val allCrossRefs = userCardDao.getAllFolderCardCrossRefs().firstOrNull() ?: emptyList()

        val userCards = if (folderIds == null) {
            allUserCards
        } else {
            // Filter user cards that are in the selected folders
            val userCardIdsInFolders = allCrossRefs
                .filter { folderIds.contains(it.folderId) }
                .map { it.userCardId }
                .toSet()
            allUserCards.filter { userCardIdsInFolders.contains(it.userCard.id) }
        }

        val folderDtos = folders.map { FolderDto(it.id, it.name, it.icon, it.color) }
        val userCardDtos = userCards.map { cardWithDetails ->
            val userCard = cardWithDetails.userCard
            UserCardDto(
                cardId = userCard.cardId,
                quantity = userCard.quantity,
                condition = userCard.condition,
                printing = userCard.printing,
                finish = userCard.finish,
                manualPrice = userCard.manualPrice,
                dateAdded = userCard.dateAdded,
                folderIds = allCrossRefs.filter { it.userCardId == userCard.id }.map { it.folderId }
            )
        }

        val export = CollectionExportDto(
            folders = folderDtos,
            userCards = userCardDtos
        )

        collectionExportAdapter.toJson(export)
    }

    suspend fun importCollectionFromJson(json: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val export = collectionExportAdapter.fromJson(json) ?: throw Exception("Invalid JSON")
            
            // Map old folder IDs to new folder IDs
            val folderIdMap = mutableMapOf<Long, Long>()
            export.folders.forEach { folderDto ->
                val newId = folderDao.insertFolder(FolderEntity(
                    name = folderDto.name,
                    icon = folderDto.icon,
                    color = folderDto.color
                ))
                folderIdMap[folderDto.id] = newId
            }

            // Import user cards and their folder associations
            export.userCards.forEach { cardDto ->
                // Ensure the card entity exists (or at least the set is synced)
                val setId = cardDto.cardId.substringBefore("-")
                ensureSetIsSynced(setId)
                
                // We don't necessarily need to download the whole set, but the card detail is nice
                if (cardDao.getCardById(cardDto.cardId) == null) {
                    val remoteCard = getCardDetail(cardDto.cardId)
                    if (remoteCard != null) {
                        val entity = remoteCard.toEntity(setId)
                        cardDao.insertCards(listOf(entity))
                    }
                }

                val userCardId = userCardDao.insertUserCard(UserCardEntity(
                    cardId = cardDto.cardId,
                    quantity = cardDto.quantity,
                    condition = cardDto.condition,
                    printing = cardDto.printing,
                    finish = cardDto.finish,
                    manualPrice = cardDto.manualPrice,
                    dateAdded = cardDto.dateAdded
                ))

                // Restore folder associations
                val newFolderIds = cardDto.folderIds.mapNotNull { folderIdMap[it] }
                if (newFolderIds.isNotEmpty()) {
                    val crossRefs = newFolderIds.map { FolderCardCrossRef(folderId = it, userCardId = userCardId) }
                    userCardDao.insertFolderCardCrossRefs(crossRefs)
                }
            }
        }
    }

    suspend fun takeSnapshot() = withContext(ioDispatcher) {
        val userCards = userCardDao.getAllUserCardsWithDetails().firstOrNull() ?: return@withContext
        val allPrices = priceDao.getAllPrices().firstOrNull() ?: emptyList()
        val allVintagePrices = priceDao.getAllVintagePrices().firstOrNull() ?: emptyList()

        val priceMap = allPrices.associateBy { "${it.cardId}_${it.finish}_${it.condition}" }
        val vintagePriceMap =
            allVintagePrices.associateBy { "${it.cardId}_${it.finish}_${it.printing}_${it.condition}" }

        var totalValue = 0.0
        var totalQuantity = 0

        userCards.forEach { details ->
            val userCard = details.userCard
            val card = details.card

            totalQuantity += userCard.quantity

            val price = if (userCard.manualPrice != null) {
                userCard.manualPrice
            } else if (VintageSets.isVintageSet(card.setId)) {
                val key =
                    "${userCard.cardId}_${userCard.finish}_${userCard.printing}_${userCard.condition}"
                vintagePriceMap[key]?.marketPrice ?: 0.0
            } else {
                val key = "${userCard.cardId}_${userCard.finish}_${userCard.condition}"
                priceMap[key]?.marketPrice ?: 0.0
            }
            totalValue += price * userCard.quantity
        }

        collectionSnapshotDao.insertSnapshot(
            CollectionSnapshotEntity(
                totalValue = totalValue,
                cardCount = totalQuantity
            )
        )
    }

    fun getAllSnapshots(): Flow<List<CollectionSnapshotEntity>> =
        collectionSnapshotDao.getAllSnapshots()

    suspend fun addCardToWishlist(
        card: TcgDexCard,
        wishlistCardEntity: WishlistCardEntity
    ): Long {
        val setId = card.id.substringBefore("-")
        ensureSetIsSynced(setId)

        if (cardDao.getCardById(card.id) == null) {
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

        val id = wishlistDao.insertWishlistCard(wishlistCardEntity.copy(cardId = card.id))
        updateCardPrice(card.id)
        return id
    }

    suspend fun removeCardFromWishlist(wishlistId: Long) {
        wishlistDao.deleteWishlistCard(wishlistId)
    }

    suspend fun moveWishlistCardToCollection(wishlistId: Long): Long? {
        return withContext(ioDispatcher) {
            val wishlistCard =
                wishlistDao.getWishlistCardByIdSync(wishlistId) ?: return@withContext null

            val userCardId = addUserCard(
                card = getCardDetail(wishlistCard.cardId) ?: return@withContext null,
                userCardEntity = UserCardEntity(
                    cardId = wishlistCard.cardId,
                    quantity = wishlistCard.quantity,
                    condition = wishlistCard.condition,
                    printing = wishlistCard.printing,
                    finish = wishlistCard.finish
                )
            )

            wishlistDao.deleteWishlistCard(wishlistId)
            userCardId
        }
    }
}
