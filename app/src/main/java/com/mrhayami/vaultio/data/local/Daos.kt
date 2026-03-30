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

    @Query("SELECT * FROM sets")
    suspend fun getSetsSync(): List<SetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<SetEntity>): List<Long>

    @Query("UPDATE sets SET isDownloaded = :isDownloaded WHERE id = :setId")
    suspend fun updateDownloadStatus(setId: String, isDownloaded: Boolean): Int

    @Query("SELECT * FROM sets WHERE id = :setId")
    suspend fun getSetById(setId: String): SetEntity?
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE setId = :setId")
    fun getCardsBySet(setId: String): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>): List<Long>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: String): CardEntity?

    @Query("SELECT * FROM cards WHERE localId = :localId")
    suspend fun getCardsByLocalId(localId: String): List<CardEntity>

    /**
     * Searches for cards by localId AND cross-checks the parent set's card count.
     * Used by the scanner to narrow down the correct set when the OCR reads the
     * "X/TOTAL" collector number (e.g. 123/191 → only sets with 191 cards).
     */
    @Query("""
        SELECT cards.* FROM cards
        INNER JOIN sets ON cards.setId = sets.id
        WHERE cards.localId = :localId
        AND (sets.totalCards = :total OR sets.officialCards = :total)
    """)
    suspend fun getCardsByLocalIdAndSetTotal(localId: String, total: Int): List<CardEntity>

    @Query("SELECT * FROM cards WHERE name LIKE '%' || :name || '%'")
    suspend fun searchCardsByName(name: String): List<CardEntity>

    @Query("DELETE FROM cards WHERE setId = :setId AND id NOT IN (SELECT cardId FROM user_cards)")
    suspend fun deleteCardsBySet(setId: String): Int

    @Query("DELETE FROM cards WHERE id NOT IN (SELECT cardId FROM user_cards)")
    suspend fun deleteAllUnusedCards(): Int
    
    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int
}

@Dao
interface UserCardDao {
    @Transaction
    @Query("""
        SELECT
            user_cards.*,
            c.id as card_id, c.localId as card_localId, c.name as card_name, c.image as card_image, c.setId as card_setId, c.rarity as card_rarity, c.category as card_category, c.types as card_types, c.dexId as card_dexId, c.tcgPlayerId as card_tcgPlayerId, c.lastUpdated as card_lastUpdated,
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
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
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
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
            s.id as set_id, s.name as set_name, s.series as set_series, s.logo as set_logo, s.symbol as set_symbol, s.totalCards as set_totalCards, s.officialCards as set_officialCards, s.releaseDate as set_releaseDate, s.isDownloaded as set_isDownloaded, s.lastUpdated as set_lastUpdated
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
    suspend fun deleteUserCard(userCardId: Long): Int

    @Query("DELETE FROM user_cards WHERE id IN (:userCardIds)")
    suspend fun deleteUserCards(userCardIds: List<Long>): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderCardCrossRef(crossRef: FolderCardCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolderCardCrossRefs(crossRefs: List<FolderCardCrossRef>): List<Long>

    @Query("DELETE FROM folder_cards WHERE userCardId = :userCardId AND folderId = :folderId")
    suspend fun removeCardFromFolder(userCardId: Long, folderId: Long): Int

    @Query("SELECT * FROM folder_cards")
    fun getAllFolderCardCrossRefs(): Flow<List<FolderCardCrossRef>>
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity): Int

    @Delete
    suspend fun deleteFolder(folder: FolderEntity): Int
}

@Dao
interface PriceDao {
    @Query("SELECT * FROM prices WHERE cardId = :cardId")
    fun getPricesForCard(cardId: String): Flow<List<PriceEntity>>

    @Query("SELECT * FROM prices")
    fun getAllPrices(): Flow<List<PriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<PriceEntity>): List<Long>

    @Query("SELECT * FROM vintage_prices WHERE cardId = :cardId")
    fun getVintagePricesForCard(cardId: String): Flow<List<VintagePriceEntity>>

    @Query("SELECT * FROM vintage_prices")
    fun getAllVintagePrices(): Flow<List<VintagePriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVintagePrices(prices: List<VintagePriceEntity>): List<Long>
}

@Dao
interface ApiUsageDao {
    @Query("SELECT * FROM api_usage WHERE date = :date")
    suspend fun getUsageForDate(date: String): ApiUsageEntity?

    @Query("SELECT * FROM api_usage WHERE date = :date")
    fun getUsageFlow(date: String): Flow<ApiUsageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: ApiUsageEntity): Long
}

@Dao
interface TelemetryDao {
    @Insert
    suspend fun insertLog(log: TelemetryLogEntity): Long

    @Query("SELECT * FROM telemetry_log ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<TelemetryLogEntity>>
}
