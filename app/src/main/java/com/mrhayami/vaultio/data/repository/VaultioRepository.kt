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
                SetEntity(
                    id = it.id,
                    name = it.name,
                    series = it.series,
                    logo = it.logo?.let { l -> if (l.endsWith(".png")) l else "$l.png" },
                    symbol = it.symbol?.let { s -> if (s.endsWith(".png")) s else "$s.png" },
                    totalCards = it.cardCount.total,
                    releaseDate = it.releaseDate
                )
            }
            setDao.insertSets(entities)
        } catch (e: Exception) {
            e.printStackTrace()
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
            e.printStackTrace()
        }
    }

    suspend fun deleteDownloadedSet(setId: String) {
        setDao.updateDownloadStatus(setId, false)
    }

    suspend fun searchTcgDex(query: String): List<TcgDexCard> {
        return try {
            if (query.isBlank()) return emptyList()
            tcgDexApi.searchCards("$query*")
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun searchTcgDexByLocalId(localId: String): List<TcgDexCard> {
        return try {
            tcgDexApi.searchCardsByLocalId(localId)
        } catch (e: Exception) {
            e.printStackTrace()
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
                        logo = remoteSet.logo?.let { l -> if (l.endsWith(".png")) l else "$l.png" },
                        symbol = remoteSet.symbol?.let { s -> if (s.endsWith(".png")) s else "$s.png" },
                        totalCards = remoteSet.cardCount.total,
                        releaseDate = remoteSet.releaseDate
                    )
                    setDao.insertSets(listOf(setEntity!!))
                } else {
                    setDao.insertSets(listOf(SetEntity(
                        id = setId,
                        name = "Unknown Set",
                        series = null,
                        logo = null,
                        symbol = null,
                        totalCards = 0,
                        releaseDate = null
                    )))
                }
            } catch (e: Exception) {
                Log.e("Vaultio", "Failed to fetch sets list", e)
            }
        }

        val existingCard = cardDao.getCardById(card.id)
        if (existingCard == null || existingCard.dexId == null) {
            val fullCard = try {
                Log.d("Vaultio", "Fetching details for ${card.id} to get dexId")
                tcgDexApi.getCardDetail(card.id)
            } catch (e: Exception) {
                Log.e("Vaultio", "Failed to fetch card details", e)
                card
            }
            
            val dexId = fullCard.dexId?.firstOrNull()?.toString()
            Log.d("Vaultio", "Resolved dexId: $dexId for ${fullCard.name}")

            val cardEntity = CardEntity(
                id = fullCard.id,
                localId = fullCard.localId,
                name = fullCard.name,
                image = fullCard.image,
                setId = setId,
                rarity = fullCard.rarity,
                category = fullCard.category,
                types = null,
                dexId = dexId,
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
