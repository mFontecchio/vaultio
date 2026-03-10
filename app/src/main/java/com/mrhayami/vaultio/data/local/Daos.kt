package com.mrhayami.vaultio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CardWithDetails(
    @Embedded
    val userCard: UserCardEntity,
    @Embedded(prefix = "card_")
    val card: CardEntity,
    @Embedded(prefix = "set_")
    val set: SetEntity
)

@Dao
interface SetDao {
    @Query("SELECT * FROM sets ORDER BY releaseDate DESC")
    fun getAllSets(): Flow<List<SetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetEntity>)

    @Query("UPDATE sets SET isDownloaded = :isDownloaded WHERE id = :setId")
    suspend fun updateDownloadStatus(setId: String, isDownloaded: Boolean)

    @Query("SELECT * FROM sets WHERE id = :setId")
    suspend fun getSetById(setId: String): SetEntity?
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE setId = :setId")
    fun getCardsBySet(setId: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: String): CardEntity?
}

@Dao
interface UserCardDao {
    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.tcgPlayerId as card_tcgPlayerId, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM user_cards
        INNER JOIN cards c ON user_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        WHERE user_cards.id = :userCardId
        """)
    fun getUserCardById(userCardId: Long): Flow<CardWithDetails?>

    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.tcgPlayerId as card_tcgPlayerId, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM user_cards
        INNER JOIN cards c ON user_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        """)
    fun getAllUserCardsWithDetails(): Flow<List<CardWithDetails>>

    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.tcgPlayerId as card_tcgPlayerId, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
        FROM user_cards
        INNER JOIN cards c ON user_cards.cardId = c.id
        INNER JOIN sets s ON c.setId = s.id
        INNER JOIN folder_cards fc ON user_cards.id = fc.userCardId
        WHERE fc.folderId = :folderId
        """)
    fun getUserCardsByFolder(folderId: Long): Flow<List<CardWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserCard(userCard: UserCardEntity): Long

    @Query("DELETE FROM user_cards WHERE id = :userCardId")
    suspend fun deleteUserCard(userCardId: Long)

    @Query("DELETE FROM user_cards WHERE id IN (:userCardIds)")
    suspend fun deleteUserCards(userCardIds: List<Long>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderCardCrossRef(crossRef: FolderCardCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolderCardCrossRefs(crossRefs: List<FolderCardCrossRef>)

    @Query("DELETE FROM folder_cards WHERE userCardId = :userCardId AND folderId = :folderId")
    suspend fun removeCardFromFolder(userCardId: Long, folderId: Long)
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM prices WHERE cardId = :cardId")
    fun getPricesForCard(cardId: String): Flow<List<PriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<PriceEntity>)

    @Query("SELECT * FROM vintage_prices WHERE cardId = :cardId")
    fun getVintagePricesForCard(cardId: String): Flow<List<VintagePriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVintagePrices(prices: List<VintagePriceEntity>)
}

@Dao
interface ApiUsageDao {
    @Query("SELECT * FROM api_usage WHERE date = :date")
    suspend fun getUsageForDate(date: String): ApiUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: ApiUsageEntity)
}

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insertLog(log: TelemetryLogEntity)

    @Query("SELECT * FROM telemetry_log ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<TelemetryLogEntity>>
}
