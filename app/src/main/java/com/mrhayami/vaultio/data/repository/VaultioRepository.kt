package com.mrhayami.vaultio.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
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
import com.mrhayami.vaultio.data.remote.JustTcgBatchRequestItem
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val TAG = "VaultioRepository"

/**
 * Main integration hub for the app. Handles data orchestration between Room, Retrofit,
 * DataStore, and external APIs.
 */
class VaultioRepository(
    private val context: Context,
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
    private val imageLoader: ImageLoader,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val moshi = Moshi.Builder().build()
    private val listIntAdapter = moshi.adapter<List<Int>>(
        Types.newParameterizedType(List::class.java, Int::class.javaObjectType)
    )
    private val collectionExportAdapter = moshi.adapter(CollectionExportDto::class.java)

    // region Catalog Observables
    val allSets: Flow<List<SetEntity>> = setDao.getAllSets()
    val allUserCards: Flow<List<CardWithDetails>> = userCardDao.getAllUserCardsWithDetails()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()
    val allFolderCardCrossRefs: Flow<List<FolderCardCrossRef>> = userCardDao.getAllFolderCardCrossRefs()
    val allPrices: Flow<List<PriceEntity>> = priceDao.getAllPrices()
    val allVintagePrices: Flow<List<VintagePriceEntity>> = priceDao.getAllVintagePrices()
    val allWishlistCards: Flow<List<WishlistCardWithDetails>> = wishlistDao.getAllWishlistCards()
    // endregion

    // region User Card Retrieval
    fun getUserCardById(userCardId: Long): Flow<CardWithDetails?> = userCardDao.getUserCardById(userCardId)

    suspend fun getUserCardByIdSync(userCardId: Long): CardWithDetails? = userCardDao.getUserCardByIdSync(userCardId)

    suspend fun getLastUserCardByCardId(cardId: String) = userCardDao.getLastUserCardByCardId(cardId)

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
            SortMode.VALUE -> "COALESCE(uc.manualPrice, 0.0)"
            SortMode.DATE_ADDED -> "uc.dateAdded"
            SortMode.RARITY -> "c.rarity"
            SortMode.QUANTITY -> "uc.quantity"
            SortMode.NUMBER -> "c.localId"
        }

        val direction = if (sortDirection == SortDirection.DESCENDING) "DESC" else "ASC"

        if (sortMode != SortMode.VALUE) {
            queryBuilder.append(" ORDER BY $orderBy $direction")
        }

        val query = SimpleSQLiteQuery(queryBuilder.toString(), args.toTypedArray())
        return userCardDao.getFilteredUserCards(query)
    }
    // endregion

    // region Set Management
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

    suspend fun getNewSets(): List<SetEntity> {
        return try {
            val remoteSets = tcgDexApi.getSets()
            val localSets = setDao.getSetsSync()
            val localIds = localSets.map { it.id }.toSet()

            val newSets = remoteSets.filter { it.id !in localIds }
            if (newSets.isNotEmpty()) {
                val newEntities = newSets.map { it.toEntity(false) }
                setDao.insertSets(newEntities)
                newEntities
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for new sets", e)
            emptyList()
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
            
            val currentSet = setDao.getSetById(setId)
            if (currentSet?.logo?.contains("http") == false) {
                refreshSets() 
            }
        }.onFailure { e ->
            Log.e(TAG, "Error download set $setId", e)
        }
    }

    suspend fun deleteDownloadedSet(setId: String) {
        setDao.updateDownloadStatus(setId, false)
        cardDao.deleteCardsBySet(setId)
    }
    // endregion

    // region Card & Scanner Helpers
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

    fun observeCatalogCardCount(): Flow<Int> = cardDao.observeCardCount()

    suspend fun searchLocalCards(localId: String): List<CardEntity> = cardDao.getCardsByLocalId(localId)

    suspend fun searchLocalCardsWithTotal(localId: String, total: Int): List<CardEntity> = 
        cardDao.getCardsByLocalIdAndSetTotal(localId, total)

    suspend fun updateCardPHash(cardId: String, pHash: Long) = withContext(ioDispatcher) {
        cardDao.updateCardPHash(cardId, pHash)
    }

    /**
     * Fetches a bitmap from a URL using Coil's ImageLoader for caching and performance.
     */
    suspend fun fetchBitmapFromUrl(url: String): Bitmap? = withContext(ioDispatcher) {
        try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else null
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
    // endregion

    // region Collection Operations
    /**
     * Ensures a catalog [CardEntity] exists with resolved dex metadata when possible.
     * Re-enriches when the row is missing or [CardEntity.dexId] is null (e.g. set-download stubs).
     * Preserves an existing [CardEntity.pHash] on REPLACE.
     */
    suspend fun ensureCatalogCardWithDex(
        cardId: String,
        seed: TcgDexCard? = null
    ) {
        val existing = cardDao.getCardById(cardId)
        if (existing?.dexId != null) return

        val setId = cardId.substringBefore("-")
        val seedOrStub = seed ?: TcgDexCard(
            id = cardId,
            localId = existing?.localId ?: cardId.substringAfter("-"),
            name = existing?.name ?: cardId,
            image = existing?.image,
            rarity = existing?.rarity,
            category = existing?.category,
            dexId = null,
            types = existing?.types?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
        )
        val fullCard = fetchFullCardDetails(seedOrStub)
        val (dexIdString, dexIdsJson) = resolveDexIds(fullCard)

        val cardEntity = CardEntity(
            id = fullCard.id,
            localId = fullCard.localId,
            name = fullCard.name,
            image = fullCard.image ?: existing?.image,
            setId = setId,
            rarity = fullCard.rarity ?: existing?.rarity,
            category = fullCard.category ?: existing?.category,
            types = fullCard.types?.joinToString(",") ?: existing?.types,
            dexId = dexIdString,
            dexIds = dexIdsJson,
            pokemonName = PokemonUtils.extractPokemonName(fullCard.name)
                .takeIf { it.isNotBlank() } ?: existing?.pokemonName,
            tcgPlayerId = extractTcgPlayerId(fullCard.pricing?.tcgplayer?.url)
                ?: existing?.tcgPlayerId,
            pHash = existing?.pHash
        )
        cardDao.insertCards(listOf(cardEntity))
    }

    private suspend fun backfillMissingDexIdsForOwnedCards() {
        val missingIds = cardDao.getOwnedCardIdsMissingDex()
        for (cardId in missingIds) {
            runCatching { ensureCatalogCardWithDex(cardId) }
                .onFailure { Log.e(TAG, "Failed to backfill dex for $cardId", it) }
        }
    }

    suspend fun addUserCard(
        card: TcgDexCard,
        userCardEntity: UserCardEntity,
        folderIds: List<Long> = emptyList()
    ): Long {
        Log.d("Vaultio", "Adding card to collection: ${card.name} (${card.id})")
        val setId = card.id.substringBefore("-")
        ensureSetIsSynced(setId)

        ensureCatalogCardWithDex(card.id, seed = card)

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
            val localMatch = cardDao.getCardsByPokemonName(normalizedName).firstOrNull()
                ?: cardDao.searchCardsByName(normalizedName).find { it.dexId != null }

            if (localMatch != null) {
                dexIdString = localMatch.dexId
                dexIdsJson = localMatch.dexIds
            } else {
                val networkDex = attemptNetworkDexRecovery(normalizedName)
                if (networkDex != null) {
                    dexIdString = networkDex.first
                    dexIdsJson = networkDex.second
                } else {
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

            userCardDao.updateUserCard(original.copy(quantity = original.quantity - 1))

            val existing = userCardDao.findExistingUserCard(
                cardId = original.cardId,
                condition = newCondition,
                printing = newPrinting,
                finish = newFinish
            )

            val newId = if (existing != null) {
                userCardDao.updateUserCard(existing.copy(quantity = existing.quantity + 1))
                existing.id
            } else {
                val newEntry = UserCardEntity(
                    cardId = original.cardId,
                    quantity = 1,
                    condition = newCondition,
                    printing = newPrinting,
                    finish = newFinish,
                    dateAdded = System.currentTimeMillis()
                )
                val insertedId = userCardDao.insertUserCard(newEntry)
                val originalFolders = userCardDao.getFolderIdsForUserCardSync(userCardId)
                if (originalFolders.isNotEmpty()) {
                    userCardDao.insertFolderCardCrossRefs(originalFolders.map { 
                        FolderCardCrossRef(folderId = it, userCardId = insertedId) 
                    })
                }
                insertedId
            }
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
    // endregion

    // region Folder Management
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
    // endregion

    // region Pricing Operations
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

    /**
     * Updates prices for a batch of cards efficiently using JustTCG's batch API
     * where possible. Fallbacks to individual TCGdex calls if necessary.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    suspend fun updatePricesBatch(cards: List<CardEntity>) = withContext(ioDispatcher) {
        val apiKey = getJustTcgApiKey()
        val canUseJustTcg = canUseJustTcg() && apiKey != null
        
        val (vintage, modern) = cards.partition { VintageSets.isVintageSet(it.setId) }

        // 1. Process Modern Cards
        if (canUseJustTcg) {
            val (batchable, individual) = modern.partition { it.tcgPlayerId != null }

            batchable.chunked(50).forEach { chunk ->
                runCatching {
                    val startTime = System.currentTimeMillis()
                    val items =
                        chunk.map { JustTcgBatchRequestItem(tcgplayerId = it.tcgPlayerId!!) }
                    val response = justTcgApi.getCardsBatch(apiKey!!, items)
                    logTelemetry(
                        "justtcg",
                        "cards/batch",
                        200,
                        System.currentTimeMillis() - startTime
                    )
                    syncApiUsage(response.metadata)

                    response.data.forEach { jCard ->
                        val matchingLocalCards =
                            chunk.filter { it.tcgPlayerId == jCard.tcgplayerId }
                        matchingLocalCards.forEach { localCard ->
                            val prices = jCard.variants.mapNotNull {
                                PricingUtils.mapJustTcgVariantToPrice(localCard.id, it)
                            }
                            if (prices.isNotEmpty()) priceDao.insertPrices(prices)
                        }
                    }
                }.onFailure { e ->
                    Log.e(TAG, "JustTCG batch update failed", e)
                }
            }
            individual.forEach { updateCardPrice(it.id) }
        } else {
            modern.forEach { updateCardPrice(it.id) }
        }

        // 2. Process Vintage Cards
        vintage.forEach { updateVintageCardPrice(it) }
    }
    // endregion

    // region API Usage & Telemetry
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
    // endregion

    // region Import/Export & Snapshots
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
            val folderIdMap = mutableMapOf<Long, Long>()
            export.folders.forEach { folderDto ->
                val newId = folderDao.insertFolder(FolderEntity(
                    name = folderDto.name,
                    icon = folderDto.icon,
                    color = folderDto.color
                ))
                folderIdMap[folderDto.id] = newId
            }

            export.userCards.forEach { cardDto ->
                val setId = cardDto.cardId.substringBefore("-")
                ensureSetIsSynced(setId)
                ensureCatalogCardWithDex(cardDto.cardId)

                val userCardId = userCardDao.insertUserCard(UserCardEntity(
                    cardId = cardDto.cardId,
                    quantity = cardDto.quantity,
                    condition = cardDto.condition,
                    printing = cardDto.printing,
                    finish = cardDto.finish,
                    manualPrice = cardDto.manualPrice,
                    dateAdded = cardDto.dateAdded
                ))

                val newFolderIds = cardDto.folderIds.mapNotNull { folderIdMap[it] }
                if (newFolderIds.isNotEmpty()) {
                    val crossRefs = newFolderIds.map { FolderCardCrossRef(folderId = it, userCardId = userCardId) }
                    userCardDao.insertFolderCardCrossRefs(crossRefs)
                }
            }

            backfillMissingDexIdsForOwnedCards()
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
    // endregion

    // region Wishlist Operations
    suspend fun addCardToWishlist(
        card: TcgDexCard,
        wishlistCardEntity: WishlistCardEntity
    ): Long {
        val setId = card.id.substringBefore("-")
        ensureSetIsSynced(setId)

        ensureCatalogCardWithDex(card.id, seed = card)

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
    // endregion
}
