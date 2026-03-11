package com.mrhayami.vaultio.data.repository

import android.util.Log
import com.mrhayami.vaultio.data.local.*
import com.mrhayami.vaultio.data.remote.JustTcgApi
import com.mrhayami.vaultio.data.remote.TcgDexApi
import com.mrhayami.vaultio.data.remote.TcgDexCard
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VaultioRepository(
    private val setDao: SetDao,
    private val cardDao: CardDao,
    private val userCardDao: UserCardDao,
    private val folderDao: FolderDao,
    private val priceDao: PriceDao,
    private val apiUsageDao: ApiUsageDao,
    private val telemetryDao: TelemetryDao,
    private val tcgDexApi: TcgDexApi,
    val justTcgApi: JustTcgApi
) {
    val allSets: Flow<List<SetEntity>> = setDao.getAllSets()
    val allUserCards: Flow<List<CardWithDetails>> = userCardDao.getAllUserCardsWithDetails()
    val allFolders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    fun getUserCardById(userCardId: Long): Flow<CardWithDetails?> {
        return userCardDao.getUserCardById(userCardId)
    }

    fun getUserCardsByFolder(folderId: Long): Flow<List<CardWithDetails>> {
        return userCardDao.getUserCardsByFolder(folderId)
    }

    fun getPricesForCard(cardId: String): Flow<List<PriceEntity>> {
        return priceDao.getPricesForCard(cardId)
    }

    suspend fun refreshSets() {
        try {
            val remoteSets = tcgDexApi.getSets()
            val entities = remoteSets.map {
                val setId = it.id
                val rawLogo = it.logo ?: "https://assets.tcgdex.net/en/sets/$setId/logo"
                val rawSymbol = it.symbol ?: "https://assets.tcgdex.net/en/sets/$setId/symbol"

                SetEntity(
                    id = setId,
                    name = it.name,
                    series = it.series,
                    logo = ensureImageUrl(rawLogo),
                    symbol = ensureImageUrl(rawSymbol),
                    totalCards = it.cardCount.total,
                    officialCards = it.cardCount.official,
                    releaseDate = it.releaseDate
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
                    types = null,
                    dexId = it.dexId?.firstOrNull()?.toString(),
                    tcgPlayerId = extractTcgPlayerId(it.tcgplayer?.url)
                )
            }
            cardDao.insertCards(cardEntities)
            setDao.updateDownloadStatus(setId, true)
        } catch (e: Exception) {
            Log.e("VaultioRepository", "Error downloading set $setId", e)
        }
    }

    suspend fun deleteDownloadedSet(setId: String) {
        setDao.updateDownloadStatus(setId, false)
        cardDao.deleteCardsBySet(setId)
    }

    suspend fun searchLocalCards(localId: String): List<CardEntity> {
        return cardDao.getCardsByLocalId(localId)
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

    suspend fun addUserCard(card: TcgDexCard, userCardEntity: UserCardEntity) {
        val setId = card.id.substringBefore("-")
        var setEntity = setDao.getSetById(setId)
        if (setEntity == null) {
            try {
                val remoteSets = tcgDexApi.getSets()
                val remoteSet = remoteSets.find { it.id == setId }
                if (remoteSet != null) {
                    setEntity = SetEntity(
                        id = remoteSet.id,
                        name = remoteSet.name,
                        series = remoteSet.series,
                        logo = ensureImageUrl(remoteSet.logo ?: "https://assets.tcgdex.net/en/sets/$setId/logo"),
                        symbol = ensureImageUrl(remoteSet.symbol ?: "https://assets.tcgdex.net/en/sets/$setId/symbol"),
                        totalCards = remoteSet.cardCount.total,
                        officialCards = remoteSet.cardCount.official,
                        releaseDate = remoteSet.releaseDate
                    )
                    setDao.insertSets(listOf(setEntity!!))
                }
            } catch (e: Exception) { Log.e("Vaultio", "Failed to sync set", e) }
        }

        val existingCard = cardDao.getCardById(card.id)
        
        // If the card doesn't exist or is missing its dexId, fetch full details
        if (existingCard == null || (existingCard.dexId == null && card.dexId == null)) {
            val fullCard = try {
                Log.d("Vaultio", "Fetching details for ${card.id} to ensure dexId presence")
                tcgDexApi.getCardDetail(card.id)
            } catch (e: Exception) {
                Log.e("Vaultio", "Failed to fetch card details", e)
                card
            }

            val cardEntity = CardEntity(
                id = fullCard.id,
                localId = fullCard.localId,
                name = fullCard.name,
                image = fullCard.image,
                setId = setId,
                rarity = fullCard.rarity,
                category = fullCard.category,
                types = null,
                dexId = fullCard.dexId?.firstOrNull()?.toString(),
                tcgPlayerId = extractTcgPlayerId(fullCard.tcgplayer?.url)
            )
            cardDao.insertCards(listOf(cardEntity))
        }
        userCardDao.insertUserCard(userCardEntity.copy(cardId = card.id))
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

    suspend fun updatePrice(price: PriceEntity) {
        priceDao.insertPrices(listOf(price))
    }

    suspend fun getApiUsage(): Int {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return apiUsageDao.getUsageForDate(today)?.count ?: 0
    }

    suspend fun incrementApiUsage() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currentUsage = apiUsageDao.getUsageForDate(today)
        if (currentUsage == null) {
            apiUsageDao.insertUsage(ApiUsageEntity(date = today, count = 1))
        } else {
            apiUsageDao.insertUsage(currentUsage.copy(count = currentUsage.count + 1))
        }
    }
}
